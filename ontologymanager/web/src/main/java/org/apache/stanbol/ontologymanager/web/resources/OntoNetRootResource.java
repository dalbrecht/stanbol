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
package org.apache.stanbol.ontologymanager.web.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.owl.util.URIUtils;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OntologyLoadingException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyNetworkMultiplexer;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.MGraphNetworkMultiplexer;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.OntologyUtils;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.web.util.OntologyStatsResource;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.RemoveImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * Provides the basic HTTP methods for storing and managing ontologies regardless of them belonging to a
 * specific network, scope or session.
 * 
 * @author anuzzolese, alexdma
 * 
 */

@Path("/ontonet")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class OntoNetRootResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    protected ONManager onManager;

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    protected OntologyProvider<?> ontologyProvider;

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    protected RegistryManager registryManager;

    protected SessionManager sessionManager;

    public OntoNetRootResource(@Context ServletContext servletContext) {
        super();
        this.servletContext = servletContext;
        this.ontologyProvider = (OntologyProvider<?>) ContextHelper.getServiceFromContext(
            OntologyProvider.class, servletContext);
        this.onManager = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
        this.sessionManager = (SessionManager) ContextHelper.getServiceFromContext(SessionManager.class,
            servletContext);
        this.registryManager = (RegistryManager) ContextHelper.getServiceFromContext(RegistryManager.class,
            servletContext);
    }

    /*
     * TODO before implementing removal, we need OWL dependency checks. Also, this is quite a strong method
     * and would be best implemented with RESTful authentication.
     */
    // @DELETE
    public Response clear(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /*
     * TODO before implementing removal, we need OWL dependency checks.
     */
    // @DELETE
    // @Path("/{ontologyId:.+}")
    public Response deleteOntology(@PathParam("ontologyId") String ontologyid, @Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    private MGraph getGraph(String ontologyId, boolean merged) {
        long before = System.currentTimeMillis();
        IRI iri = URIUtils.sanitize(IRI.create(ontologyId));
        log.debug("Will try to retrieve ontology {} from provider.", iri);
        /*
         * Export directly to MGraph since the OWLOntologyWriter uses (de-)serializing converters for the
         * other formats.
         * 
         * Use oTemp for the "real" graph and o for the graph that will be exported. This is due to the fact
         * that in o we want to change import statements, but we do not want these changes to be stored
         * permanently.
         */
        MGraph o = null, oTemp = null;
        try {
            oTemp = ontologyProvider.getStoredOntology(new OWLOntologyID(iri), MGraph.class, merged);
        } catch (Exception ex) {
            log.warn("Retrieval of ontology with ID " + iri + " failed.", ex);
        }

        if (oTemp == null) {
            log.debug("Ontology {} missing from provider. Trying libraries...", iri);
            // See if we can touch a library. TODO: replace with event model on the ontology provider.
            int minSize = -1;
            IRI smallest = null;
            for (Library lib : registryManager.getLibraries(iri)) {
                int size = lib.getChildren().length;
                if (minSize < 1 || size < minSize) {
                    smallest = lib.getIRI();
                    minSize = size;
                }
            }
            if (smallest != null) {
                log.debug("Selected library for ontology {} is {} .", iri, smallest);
                try {
                    oTemp = registryManager.getLibrary(smallest).getOntology(iri, MGraph.class);
                } catch (RegistryContentException e) {
                    log.warn("The content of library " + smallest + " could not be accessed.", e);
                }
            }
        }

        if (oTemp != null) o = new IndexedMGraph(oTemp);

        if (o == null) {
            log.debug("Ontology {} not found in any ontology provider or library.", iri);
            return null;
        }

        log.debug("Retrieved ontology {} .", iri);

        // Rewrite imports
        String uri = uriInfo.getRequestUri().toString();
        URI base = URI.create(uri.substring(0, uri.lastIndexOf(ontologyId) - 1));

        // Rewrite import statements
        /*
         * TODO manage import rewrites better once the container ID is fully configurable (i.e. instead of
         * going upOne() add "session" or "ontology" if needed).
         */
        Iterator<Triple> imports = o.filter(null, OWL.imports, null);
        Set<Triple> oldImports = new HashSet<Triple>();
        while (imports.hasNext())
            oldImports.add(imports.next());
        for (Triple t : oldImports) {
            // construct new statement
            String s = ((UriRef) t.getObject()).getUnicodeString();
            if (s.contains("::")) s = s.substring(s.indexOf("::") + 2, s.length());
            UriRef target = new UriRef(base + "/" + s);
            o.add(new TripleImpl(t.getSubject(), OWL.imports, target));
            // remove old statement
            o.remove(t);
        }
        log.debug("Exported as Clerezza Graph in {} ms. Handing over to writer.", System.currentTimeMillis()
                                                                                  - before);
        return o;
    }

    public Set<String> getHandles(OWLOntologyID ontologyId) {
        Set<String> handles = new HashSet<String>();
        if (onManager != null) for (OntologyScope scope : onManager.getRegisteredScopes())
            if (scope.getCoreSpace().hasOntology(ontologyId)
                || scope.getCustomSpace().hasOntology(ontologyId)) handles.add(scope.getID());
        if (sessionManager != null) for (String sesId : sessionManager.getRegisteredSessionIDs())
            if (sessionManager.getSession(sesId).hasOntology(ontologyId)) handles.add(sesId);
        return handles;
    }

    public Set<String> getAliases(OWLOntologyID ontologyId) {
        Set<String> aliases = new HashSet<String>();
        for (OWLOntologyID alias : ontologyProvider.listAliases(ontologyId))
            aliases.add(OntologyUtils.encode(alias));
        return aliases;
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response getManagedGraph(@PathParam("ontologyId") String ontologyId,
                                    @DefaultValue("false") @QueryParam("merge") boolean merged,
                                    @Context UriInfo uriInfo,
                                    @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else {
            TripleCollection o = getGraph(ontologyId, merged);
            rb = o == null ? Response.status(NOT_FOUND) : Response.ok(o);
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Gets the ontology with the given identifier in its version managed by the session.
     * 
     * @param sessionId
     *            the session identifier.
     * @param ontologyId
     *            the ontology identifier.
     * @param uriInfo
     * @param headers
     * @return the requested managed ontology, or {@link Status#NOT_FOUND} if either the sessionn does not
     *         exist, or the if the ontology either does not exist or is not managed.
     */
    @GET
    @Path("/{ontologyId:.+}")
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response getManagedOntology(@PathParam("ontologyId") String ontologyId,
                                       @DefaultValue("false") @QueryParam("merge") boolean merged,
                                       @Context UriInfo uriInfo,
                                       @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else {
            OWLOntology o = getOntology(ontologyId, merged);
            rb = o == null ? Response.status(NOT_FOUND) : Response.ok(o);
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces({RDF_XML, TURTLE, X_TURTLE, APPLICATION_JSON, RDF_JSON})
    public Response getMetaGraph(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(ontologyProvider.getMetaGraph(Graph.class));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public SortedSet<OWLOntologyID> getOntologies() {
        SortedSet<OWLOntologyID> filtered = new TreeSet<OWLOntologyID>();
        for (OWLOntologyID id : ontologyProvider.getPublicKeys())
            if (id != null) filtered.add(id);
        return filtered;
    }

    private OWLOntology getOntology(String ontologyId, boolean merge) {
        long before = System.currentTimeMillis();
        IRI iri = URIUtils.sanitize(IRI.create(ontologyId));
        log.debug("Will try to retrieve ontology {} from provider.", iri);
        // TODO be selective: if the ontology is small enough, use OWLOntology otherwise export to Graph.
        OWLOntology o = null;
        try {
            // XXX Guarantee that there MUST always be an entry for any decoded ontology ID submitted.
            OWLOntologyID id = OntologyUtils.decode(ontologyId);
            o = ontologyProvider.getStoredOntology(id, OWLOntology.class, merge);
        } catch (Exception ex) {
            log.warn("Retrieval of ontology with ID " + iri + " failed.", ex);
        }

        if (o == null) {
            log.debug("Ontology {} missing from provider. Trying libraries...", iri);
            // See if we can touch a library. TODO: replace with event model on the ontology provider.
            int minSize = -1;
            IRI smallest = null;
            for (Library lib : registryManager.getLibraries(iri)) {
                int size = lib.getChildren().length;
                if (minSize < 1 || size < minSize) {
                    smallest = lib.getIRI();
                    minSize = size;
                }
            }
            if (smallest != null) {
                log.debug("Selected library for ontology {} is {} .", iri, smallest);
                try {
                    o = registryManager.getLibrary(smallest).getOntology(iri, OWLOntology.class);
                } catch (RegistryContentException e) {
                    log.warn("The content of library " + smallest + " could not be accessed.", e);
                }
            }
        }

        if (o == null) {
            log.debug("Ontology {} not found in any ontology provider or library.", iri);
            return null;
        }

        log.debug("Retrieved ontology {} .", iri);
        // Rewrite import statements - no ontology collector to do it for us here.
        URI base = URI.create(getPublicBaseUri() + "ontonet/");
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();

        // TODO manage import rewrites better once the container ID is fully configurable.
        for (OWLImportsDeclaration oldImp : o.getImportsDeclarations()) {
            changes.add(new RemoveImport(o, oldImp));
            String s = oldImp.getIRI().toString();
            if (s.contains("::")) s = s.substring(s.indexOf("::") + 2, s.length());
            IRI target = IRI.create(base + s);
            changes.add(new AddImport(o, df.getOWLImportsDeclaration(target)));
        }
        o.getOWLOntologyManager().applyChanges(changes);
        log.debug("Exported as Clerezza Graph in {} ms. Handing over to writer.", System.currentTimeMillis()
                                                                                  - before);
        return o;
    }

    public int getSize(OWLOntologyID ontologyId) {
        OntologyNetworkMultiplexer desc = new MGraphNetworkMultiplexer(
                ontologyProvider.getMetaGraph(MGraph.class));
        return desc.getSize(ontologyId);
    }

    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, TEXT_PLAIN, RDF_XML, TURTLE, X_TURTLE, N3})
    public Response postOntology(FormDataMultiPart data, @Context HttpHeaders headers) {
        log.debug(" post(FormDataMultiPart data)");
        ResponseBuilder rb = null;

        IRI location = null;
        File file = null; // If found, it takes precedence over location.
        String format = null;
        for (BodyPart bpart : data.getBodyParts()) {
            log.debug("is a {}", bpart.getClass());
            if (bpart instanceof FormDataBodyPart) {
                FormDataBodyPart dbp = (FormDataBodyPart) bpart;
                String name = dbp.getName();
                if (name.equals("file")) file = bpart.getEntityAs(File.class);
                else if (name.equals("format") && !dbp.getValue().equals("auto")) format = dbp.getValue();
                else if (name.equals("url")) try {
                    URI.create(dbp.getValue()); // To throw 400 if malformed.
                    location = IRI.create(dbp.getValue());
                } catch (Exception ex) {
                    log.error("Malformed IRI for " + dbp.getValue(), ex);
                    throw new WebApplicationException(ex, BAD_REQUEST);
                }
            }
        }
        // Then add the file
        OWLOntologyID key = null;
        if (file != null && file.canRead() && file.exists()) {

            /*
             * Because the ontology provider's load method could fail after only one attempt without resetting
             * the stream, we might have to do that ourselves.
             */

            List<String> formats;
            if (format != null && !format.trim().isEmpty()) formats = Collections.singletonList(format);
            else // The RESTful API has its own list of preferred formats
            formats = Arrays.asList(new String[] {RDF_XML, TURTLE, X_TURTLE, N3, N_TRIPLE, OWL_XML,
                                                  FUNCTIONAL_OWL, MANCHESTER_OWL, RDF_JSON});
            int unsupported = 0, failed = 0;
            Iterator<String> itf = formats.iterator();
            if (!itf.hasNext()) throw new OntologyLoadingException("No suitable format found or defined.");
            do {
                String f = itf.next();
                try {
                    // Re-instantiate the stream on every attempt
                    InputStream content = new FileInputStream(file);
                    // ClerezzaOWLUtils.guessOntologyID(new FileInputStream(file), Parser.getInstance(), f);
                    key = ontologyProvider.loadInStore(content, f, true);
                } catch (UnsupportedFormatException e) {
                    log.warn(
                        "POST method failed for media type {}. This should not happen (should fail earlier)",
                        headers.getMediaType());
                    // rb = Response.status(UNSUPPORTED_MEDIA_TYPE);
                    unsupported++;
                } catch (IOException e) {
                    log.debug(">>> FAILURE format {} (I/O error)", f);
                    failed++;
                } catch (Exception e) { // SAXParseException and others
                    log.debug(">>> FAILURE format {} (parse error)", f);
                    failed++;
                }
            } while ((key == null || key.isAnonymous()) && itf.hasNext());
            if (key == null || key.isAnonymous()) {
                if (failed > 0) throw new WebApplicationException(BAD_REQUEST);
                else if (unsupported > 0) throw new WebApplicationException(UNSUPPORTED_MEDIA_TYPE);
            }
        } else if (location != null) {
            try { // Here we try every format supported by the Java API
                key = ontologyProvider.loadInStore(location, null, true);
            } catch (Exception e) {
                log.error("Failed to load ontology from " + location, e);
                throw new WebApplicationException(e, BAD_REQUEST);
            }
        } else {
            log.error("Bad request");
            log.error(" file is: {}", file);
            throw new WebApplicationException(BAD_REQUEST);
        }

        if (key != null && !key.isAnonymous()) {
            // FIXME ugly but will have to do for the time being
            String uri
            // = key.split("::")[1];
            = OntologyUtils.encode(key);
            // uri
            // = uri.substring((ontologyProvider.getGraphPrefix() + "::").length());
            if (uri != null && !uri.isEmpty()) rb = Response.seeOther(URI.create("/ontonet/" + uri));
            else rb = Response.ok();
        } else rb = Response.status(Status.INTERNAL_SERVER_ERROR);

        // rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // FIXME return an appropriate response e.g. 303
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(TEXT_HTML)
    public Response showOntology(@PathParam("ontologyId") String ontologyId, @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else {
            OWLOntologyID id = OntologyUtils.decode(ontologyId);
            OWLOntology o = getOntology(ontologyId, false);
            if (o == null) rb = Response.status(NOT_FOUND);
            else
            // try
            {
                Set<OntologyCollector> handles = new HashSet<OntologyCollector>();
                if (onManager != null) for (OntologyScope scope : onManager.getRegisteredScopes()) {
                    if (scope.getCoreSpace().hasOntology(id)) handles.add(scope.getCoreSpace());
                    if (scope.getCustomSpace().hasOntology(id)) handles.add(scope.getCustomSpace());
                }
                if (sessionManager != null) for (String sesId : sessionManager.getRegisteredSessionIDs())
                    if (sessionManager.getSession(sesId).hasOntology(id)) handles.add(sessionManager
                            .getSession(sesId));
                // ByteArrayOutputStream out = new ByteArrayOutputStream();
                // o.getOWLOntologyManager().saveOntology(o, new ManchesterOWLSyntaxOntologyFormat(), out);
                rb = Response.ok(new Viewable("ontology",
                // new OntologyPrettyPrintResource(servletContext,
                // uriInfo, out)
                        new OntologyStatsResource(servletContext, uriInfo, o, ontologyProvider
                                .listAliases(id), handles)));
                // } catch (OWLOntologyStorageException e) {
                // throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        }
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * POSTs an ontology content as application/x-www-form-urlencoded
     * 
     * @param content
     * @param headers
     * @return
     */
    @POST
    @Consumes(value = {RDF_XML, TURTLE, X_TURTLE, N3, N_TRIPLE, OWL_XML, FUNCTIONAL_OWL, MANCHESTER_OWL,
                       RDF_JSON})
    public Response storeOntology(InputStream content, @Context HttpHeaders headers) {
        long before = System.currentTimeMillis();
        ResponseBuilder rb;

        MediaType mt = headers.getMediaType();
        if (RDF_XML_TYPE.equals(mt) || TURTLE_TYPE.equals(mt) || X_TURTLE_TYPE.equals(mt)
            || N3_TYPE.equals(mt) || N_TRIPLE_TYPE.equals(mt) || RDF_JSON_TYPE.equals(mt)) {
            OWLOntologyID key = null;
            try {
                key = ontologyProvider.loadInStore(content, headers.getMediaType().toString(), true);
                rb = Response.ok();
            } catch (UnsupportedFormatException e) {
                log.warn(
                    "POST method failed for media type {}. This should not happen (should fail earlier)",
                    headers.getMediaType());
                rb = Response.status(UNSUPPORTED_MEDIA_TYPE);
            } catch (IOException e) {
                throw new WebApplicationException(e, BAD_REQUEST);
            }
            // An exception should have been thrown earlier, but just in case.
            if (key == null || key.isAnonymous()) rb = Response.status(Status.INTERNAL_SERVER_ERROR);
        } else if (OWL_XML_TYPE.equals(mt) || FUNCTIONAL_OWL_TYPE.equals(mt)
                   || MANCHESTER_OWL_TYPE.equals(mt)) {
            try {
                OntologyInputSource<OWLOntology> src = new OntologyContentInputSource(content);
                ontologyProvider.loadInStore(src.getRootOntology(), true);
                rb = Response.ok();
            } catch (OWLOntologyCreationException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        } else rb = Response.status(UNSUPPORTED_MEDIA_TYPE);

        addCORSOrigin(servletContext, rb, headers);
        Response r = rb.build();
        log.debug("POST request for ontology addition completed in {} ms with status {}.",
            (System.currentTimeMillis() - before), r.getStatus());
        return r;
    }

    public String stringForm(OWLOntologyID ontologyID) {
        return OntologyUtils.encode(ontologyID);
    }
}
