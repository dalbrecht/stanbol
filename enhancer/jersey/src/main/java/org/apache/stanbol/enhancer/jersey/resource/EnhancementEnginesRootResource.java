/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.enhancer.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancerUtils.addActiveEngines;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancerUtils.buildEnginesMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.sun.jersey.api.view.Viewable;

@Path("/enhancer/engine")
public class EnhancementEnginesRootResource extends BaseStanbolResource {

    
    private final Map<String, Entry<ServiceReference,EnhancementEngine>> engines;

    public EnhancementEnginesRootResource(@Context ServletContext context) {
        // bind the job manager by looking it up from the servlet request context
        EnhancementEngineManager engineManager = ContextHelper.getServiceFromContext(EnhancementEngineManager.class, context);
        if(engineManager == null){
            throw new WebApplicationException(new IllegalStateException(
                "The required EnhancementEngineManager Service is not available!"));
        }
        engines = buildEnginesMap(engineManager);
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext,res, headers);
        return res.build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok(new Viewable("index", this),TEXT_HTML);
        addCORSOrigin(servletContext,res, headers);
        return res.build();
    }

    @GET
    @Produces(value={APPLICATION_JSON,N3,N_TRIPLE,RDF_JSON,RDF_XML,TURTLE,X_TURTLE})
    public Response getEngines(@Context HttpHeaders headers){
        String rootUrl = uriInfo.getBaseUriBuilder().path(getRootUrl()).build().toString();
        MGraph graph = new SimpleMGraph();
        addActiveEngines(engines.values(), graph, rootUrl);
        ResponseBuilder res = Response.ok(graph);
        addCORSOrigin(servletContext,res, headers);
        return res.build();
    }

    public Collection<EnhancementEngine> getEngines(){
        Set<EnhancementEngine> engines = new HashSet<EnhancementEngine>();
        for(Entry<ServiceReference,EnhancementEngine> entry : this.engines.values()){
            engines.add(entry.getValue());
        }
        return engines;
    }
    public String getServicePid(String name){
        Entry<ServiceReference,EnhancementEngine> entry = engines.get(name);
        if(entry != null){
            return (String)entry.getKey().getProperty(Constants.SERVICE_PID);
        } else {
            return null;
        }
    }
    public Integer getServiceRanking(String name){
        Entry<ServiceReference,EnhancementEngine> entry = engines.get(name);
        Integer ranking = null;
        if(entry != null){
            ranking = (Integer)entry.getKey().getProperty(Constants.SERVICE_RANKING);
        }
        if(ranking == null){
            return new Integer(0);
        } else {
            return ranking;
        }
    }
    public Long getServiceId(String name){
        Entry<ServiceReference,EnhancementEngine> entry = engines.get(name);
        if(entry != null){
            return (Long)entry.getKey().getProperty(Constants.SERVICE_ID);
        } else {
            return null;
        }
    }
}
