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
package org.apache.stanbol.enhancer.engines.dereference.entityhub;

import java.util.concurrent.ExecutorService;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceException;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import at.newmedialab.ldpath.api.backend.RDFBackend;

public final class SitesDereferencer extends TrackingDereferencerBase<SiteManager> {
    
//    private final Logger log = LoggerFactory.getLogger(SiteDereferencer.class);
    
    private final ExecutorService executorService;

    public SitesDereferencer(BundleContext context, ExecutorService executorService){
        this(context, null, executorService);
    }
    public SitesDereferencer(BundleContext context, ServiceTrackerCustomizer customizer, ExecutorService executorService) {
        super(context, SiteManager.class, null, customizer);
        this.executorService = executorService;
    }
    
    @Override
    public boolean supportsOfflineMode() {
        return true; //can not be determined here .. return true
    }
    @Override
    public ExecutorService getExecutor() {
        return executorService;
    }
    
    @Override
    protected Representation getRepresentation(SiteManager sm, String id, boolean offlineMode) throws EntityhubException {
        Entity entity = sm.getEntity(id);
        return entity == null ? null : entity.getRepresentation();
    }
    
    @Override
    protected RDFBackend<Object> createRdfBackend(SiteManager service) {
        return new SiteManagerBackend(service);
    }

}
