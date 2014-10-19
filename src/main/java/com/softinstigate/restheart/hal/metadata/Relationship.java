/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package com.softinstigate.restheart.hal.metadata;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.softinstigate.restheart.utils.URLUtilis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uji
 */
public class Relationship
{
    private static final Logger logger = LoggerFactory.getLogger(Relationship.class);
    
    public enum TYPE { ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY };
    public enum ROLE { OWNING, INVERSE };
    
    public static final String RELATIONSHIPS_ELEMENT_NAME = "rels";
    public static final String REL_ELEMENT_NAME = "rel";
    public static final String TYPE_ELEMENT_NAME = "type";
    public static final String ROLE_ELEMENT_NAME = "role";
    public static final String TARGET_DB_ELEMENT_NAME = "target-db";
    public static final String TARGET_COLLECTION_ELEMENT_NAME = "target-coll";
    public static final String REF_ELEMENT_NAME = "ref-field";
    
    private final String rel;
    private final TYPE type;
    private final ROLE role;
    private final String targetDb;
    private final String targetCollection;
    private final String referenceField;
    
    public Relationship(String rel, TYPE type, ROLE role, String targetDb, String targetCollection, String referenceField)
    {
        this.rel = rel;
        this.type = type;
        this.role = role;
        this.targetDb = targetDb;
        this.targetCollection = targetCollection;
        this.referenceField = referenceField;
    }
    
    public Relationship(String rel, String type, String role, String targetDb, String targetCollection, String referenceField) throws InvalidMetadataException
    {
        this.rel = rel;
        
        try
        {
            this.type = TYPE.valueOf(type);
        }
        catch(IllegalArgumentException iae)
        {
            throw new InvalidMetadataException("invalid type value: " + type + ". valid values are " + Arrays.toString(TYPE.values()), iae);
        }
        
        try
        {
            this.role = ROLE.valueOf(role);
        }
        catch(IllegalArgumentException iae)
        {
            throw new InvalidMetadataException("invalid role value " + role + ". valid values are " + Arrays.toString(ROLE.values()), iae);
        }
        
        this.targetDb = targetDb;
        this.targetCollection = targetCollection;
        this.referenceField = referenceField;
    }
    
    public static List<Relationship> getFromJson(DBObject collProps) throws InvalidMetadataException
    {
        if (collProps == null)
            return null;
        
        ArrayList<Relationship> ret = new ArrayList<>();
        
        Object _rels = collProps.get(RELATIONSHIPS_ELEMENT_NAME);
        
        if (_rels == null)
            return ret;
        
        if (! (_rels instanceof BasicDBList))
            throw new InvalidMetadataException("element 'relationships' is not an array list." + _rels);
        
        BasicDBList rels = (BasicDBList) _rels;
        
        for (Object _rel: rels.toArray())
        {
            if (!(_rel instanceof DBObject))
                throw new InvalidMetadataException("element 'relationships' is not valid." + _rel);
                
            DBObject rel = (DBObject) _rel;
            ret.add(getRelFromJson(rel));
        }
        
        return ret;
    }
    
    private static Relationship getRelFromJson(DBObject content) throws InvalidMetadataException
    {
        Object _rel = content.get(REL_ELEMENT_NAME);
        Object _type = content.get(TYPE_ELEMENT_NAME);
        Object _role = content.get(ROLE_ELEMENT_NAME);
        Object _targetDb = content.get(TARGET_DB_ELEMENT_NAME);
        Object _targetCollection = content.get(TARGET_COLLECTION_ELEMENT_NAME);
        Object _referenceField = content.get(REF_ELEMENT_NAME);
        
        if (_rel == null || !(_rel instanceof String))
        {
            throw new InvalidMetadataException((_rel == null ? "missing " : "invalid ") + REL_ELEMENT_NAME + " element.");
        }
        
        if (_type == null || !(_type instanceof String))
        {
            throw new InvalidMetadataException((_type == null ? "missing " : "invalid ") + TYPE_ELEMENT_NAME + " element.");
        }
        
        if (_role == null || !(_role instanceof String))
        {
            throw new InvalidMetadataException((_role == null ? "missing " : "invalid ") + ROLE_ELEMENT_NAME + " element.");
        }
        
        if (_targetDb != null && !(_type instanceof String))
        {
            throw new InvalidMetadataException("invalid " + TARGET_DB_ELEMENT_NAME + " field.");
        }
        
        if (_targetCollection == null || !(_targetCollection instanceof String))
        {
            throw new InvalidMetadataException((_targetCollection == null ? "missing " : "invalid ") + TARGET_COLLECTION_ELEMENT_NAME + " element.");
        }
        
        if (_referenceField == null || !(_referenceField instanceof String))
        {
            throw new InvalidMetadataException((_referenceField == null ? "missing " : "invalid ") + REF_ELEMENT_NAME + " element.");
        }
       
        String rel = (String) _rel;
        String type = (String) _type;
        String role = (String) _role;
        String targetDb = (String) _targetDb;
        String targetCollection = (String) _targetCollection;
        String referenceField = (String) _referenceField;
        
        return new Relationship(rel, type, role, targetDb, targetCollection, referenceField);
    }
    
    public String getRelationshipLink(String dbName, String collName, DBObject data) throws IllegalArgumentException
    {
        Object _referenceValue = data.get(referenceField);
        String reference;
        
        // check _referenceValue
        if (role == ROLE.OWNING && _referenceValue != null)
        {
            if (type == TYPE.ONE_TO_ONE || type == TYPE.MANY_TO_ONE)
            {
                if (!(_referenceValue instanceof String))
                    throw new IllegalArgumentException("in resource " + dbName + "/" + collName + "/" + data.get("_id")
                            + " the " + type.name() + " relationship ref-field " + this.referenceField + " should be a string, but is " + _referenceValue);

                reference = (String) _referenceValue;
            }
            else
            {
                if (!(_referenceValue instanceof BasicDBList))
                    throw new IllegalArgumentException("in resource " + dbName + "/" + collName + "/" + data.get("_id")
                            + " the " + type.name() + " relationship ref-field " + this.referenceField + " should be an array, but is " + _referenceValue);

                String[] ids = ((BasicDBList)_referenceValue).toArray(new String[0]);

                for (int idx = ids.length-1; idx >=0; idx--)
                {
                    ids[idx] = "'" + ids[idx] + "'"; 
                }

                reference = Arrays.toString(ids);
            }
        }
        else // INVERSE
        {
            reference = "'" + data.get("_id").toString() + "'";
        }
        
        String db = (targetDb == null ? dbName : targetDb); 
        
        if (role == ROLE.OWNING)
        {
            if (type == TYPE.ONE_TO_ONE || type == TYPE.MANY_TO_ONE)
            {
                return URLUtilis.getUriWithDocId(db, targetCollection, reference);
            }
            else if (type == TYPE.ONE_TO_MANY || type == TYPE.MANY_TO_MANY)
            {
                return URLUtilis.getUriWithFilterMany(db, targetCollection, referenceField, reference);
            }
        }
        else
        {
            if (type == TYPE.ONE_TO_ONE || type == TYPE.ONE_TO_MANY)
            {
                return URLUtilis.getUriWithFilterOne(db, targetCollection, referenceField, reference);
            }
            else if (type == TYPE.MANY_TO_ONE || type == TYPE.MANY_TO_MANY)
            {
                return URLUtilis.getUriWithFilterManyInverse(db, targetCollection, referenceField, reference);
            }
        }

        logger.debug("returned null link. this = {}, data = {}", this, data);
        return null;
    }
    
    
    
    /**
     * @return the rel
     */
    public String getRel()
    {
        return rel;
    }

    /**
     * @return the type
     */
    public TYPE getType()
    {
        return type;
    }
    
    /**
     * @return the role
     */
    public ROLE getRole()
    {
        return role;
    }

    /**
     * @return the targetDb
     */
    public String getTargetDb()
    {
        return targetDb;
    }

    /**
     * @return the targetCollection
     */
    public String getTargetCollection()
    {
        return targetCollection;
    }

    /**
     * @return the referenceField
     */
    public String getReferenceField()
    {
        return referenceField;
    }
}
