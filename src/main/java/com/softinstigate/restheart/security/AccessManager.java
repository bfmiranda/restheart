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
package com.softinstigate.restheart.security;

import com.softinstigate.restheart.utils.RequestContext;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpServerExchange;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author uji
 */
public interface AccessManager
{
    public boolean isAllowed(HttpServerExchange exchange, RequestContext context);
    
    public HashMap<String, Set<Predicate>> getAcl();
}