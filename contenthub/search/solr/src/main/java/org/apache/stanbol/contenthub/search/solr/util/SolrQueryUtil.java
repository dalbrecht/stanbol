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
package org.apache.stanbol.contenthub.search.solr.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Constraint;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Facet;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FacetResult;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.pacaci
 * 
 */
public class SolrQueryUtil {

    private final static Logger log = LoggerFactory.getLogger(SolrQueryUtil.class);

    public static final String CONTENT_FIELD = "content_t";
    public static final String ID_FIELD = "id";
    public static final String SCORE_FIELD = "score";

    public final static String and = "AND";
    public final static String or = "OR";
    public final static String facetDelimiter = ":";
    public final static char quotation = '"';

    public final static List<Character> queryDelimiters = Arrays.asList(' ', ',');

    private static String removeFacetConstraints(String query) {
        int delimiteri = query.indexOf(facetDelimiter);
        while (delimiteri > -1) {
            int starti = delimiteri;
            for (starti = delimiteri; starti >= 0 && !queryDelimiters.contains(query.charAt(starti)); starti--)
                ;
            ++starti;

            int endi = delimiteri + 1;
            if (endi < query.length()) {
                if (query.charAt(endi) == quotation) {
                    ++endi;
                    for (; endi < query.length() && query.charAt(endi) != quotation; endi++)
                        ;
                    ++endi;
                } else {
                    for (; endi < query.length() && !queryDelimiters.contains(query.charAt(endi)); endi++)
                        ;
                }
            }
            query = query.substring(0, starti) + (endi >= query.length() ? "" : query.substring(endi));
            delimiteri = query.indexOf(facetDelimiter);
        }
        query = query.replaceAll(and + '|' + or + '|' + and.toLowerCase() + '|' + or.toLowerCase(), "");
        return query;
    }

    private static String removeSpecialCharacter(String query, char ch) {
        int starti = query.indexOf(ch);
        while (starti > -1) {
            int endi = -1;
            for (endi = starti; endi < query.length() && !queryDelimiters.contains(query.charAt(endi)); endi++)
                ;
            query = query.substring(0, starti) + (endi >= query.length() ? "" : query.substring(endi));
            starti = query.indexOf(ch);
        }
        return query;
    }

    private static String removeSpecialCharacters(String query) {
        query = query.replaceAll("[+|\\-&!\\(\\)\\{\\}\\[\\]\\*\\?\\\\]", "");
        query = removeSpecialCharacter(query, '^');
        query = removeSpecialCharacter(query, '~');
        return query;
    }

    public static String extractQueryTermFromSolrQuery(SolrParams solrQuery) {
        String queryFull = solrQuery instanceof SolrQuery ? ((SolrQuery) solrQuery).getQuery() : solrQuery
                .get(CommonParams.Q);
        queryFull = removeSpecialCharacters(queryFull);
        queryFull = removeFacetConstraints(queryFull);
        return queryFull.trim();
    }

    /**
     * This methods adds a facet field the given <code>solrQuery</code> for each facet passed in
     * <code>allAvailableFacetNames</code>. This provides obtaining information about specified facets such as
     * possible facet values, number documents of documents matching with certain values of facets, etc in the
     * search results.
     * 
     * @param solrQuery
     *            {@link SolrQuery} to be extended with facet fields
     * @param allAvailableFacetNames
     *            list of facets
     */
    public static <T> void setFacetFields(SolrQuery solrQuery, List<T> allAvailableFacetNames) {
        solrQuery.setFields("*", SCORE_FIELD);
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(1);
        if (allAvailableFacetNames != null) {
            for (T facet : allAvailableFacetNames) {
                String facetName;
                if (facet instanceof String) {
                    facetName = (String) facet;
                } else if (facet instanceof FacetResult) {
                    facetName = ((FacetResult) facet).getFacetField().getName();
                } else {
                    facetName = facet.toString();
                }
                if (SolrFieldName.CREATIONDATE.toString().equals(facetName)
                    || (!SolrFieldName.isNameReserved(facetName) && !SolrVocabulary.isNameExcluded(facetName))) {
                    solrQuery.addFacetField(facetName);
                }
            }
        }
    }

    /**
     * This method create a {@link SolrQuery} using the given parameters. <code>queryTerm</code> is the main
     * query of the solr query to be created. <code>solrServer</code> is used fetch possible facet fields of
     * the underlying Solr schema. Obtained facet names are attached to the query to obtain the corresponding
     * facet information such as possible facet values, number documents of documents matching with certain
     * values of facets, etc in the search results.
     * 
     * @param solrServer
     *            Solr server to obtain corresponding facet names
     * @param queryTerm
     *            main query term to be used in {@link SolrQuery#setQuery(String)}
     * @return {@link SolrQuery} constructed by using the given parameters
     * @throws SolrServerException
     * @throws IOException
     */
    public static SolrQuery prepareSolrQuery(SolrServer solrServer, String queryTerm) throws SolrServerException,
                                                                                     IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryTerm);
        setFacetFields(solrQuery, getAllFacetNames(solrServer));
        return solrQuery;
    }

    /**
     * This method simply wraps the given <code>queryTerm</code> in a {@link SolrQuery} instance.
     * 
     * @param queryTerm
     *            {@link String} query term to be represented as a {@link SolrQuery}
     * @return {@link SolrQuery} wrapping the given <code>queryTerm</code>
     */
    public static SolrQuery prepareSolrQuery(String queryTerm) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryTerm);
        return solrQuery;
    }

    /**
     * This method creates a {@link SolrQuery} with the given parameters. It sets the <code>queryTerm</code>
     * as the main query and for each constraint passed in the <code>constraints</code> a filter query is
     * added to the solr query.
     * 
     * @param queryTerm
     *            main query to be used in {@link SolrQuery#setQuery(String)}
     * @param allAvailableFacets
     *            {@link FacetResult}s passed in this list are used to check types of the facets.
     * @param constraints
     *            additional constraints to be applied in the {@link SolrQuery}.
     * @return {@link SolrQuery} constructed by using the given parameters
     */
    public static SolrQuery prepareSolrQuery(String queryTerm,
                                             List<FacetResult> allAvailableFacets,
                                             Map<String,List<Object>> constraints) {
        SolrQuery query = new SolrQuery();
        query.setQuery(queryTerm);
        if (constraints != null) {
            try {
                for (Entry<String,List<Object>> entry : constraints.entrySet()) {
                    String fieldName = ClientUtils.escapeQueryChars(entry.getKey());
                    String type = getFacetFieldType(fieldName, allAvailableFacets);
                    for (Object value : entry.getValue()) {
                        if (SolrVocabulary.isRangeType(type)) {
                            query.addFilterQuery(fieldName + facetDelimiter + (String) value);
                        } else {
                            query.addFilterQuery(fieldName + facetDelimiter + quotation
                                                 + ClientUtils.escapeQueryChars((String) value) + quotation);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Facet constraints could not be added to Query", e);
            }
        }
        return query;
    }

    private static String getFacetFieldType(String fieldName, List<FacetResult> allAvailableFacets) {
        for (FacetResult fr : allAvailableFacets) {
            if (fieldName.equals(fr.getFacetField().getName())) {
                return fr.getType();
            }
        }
        return "";
    }

    public static List<String> getAllFacetNames(SolrServer solrServer) throws SolrServerException,
                                                                      IOException {
        List<String> facetNames = new ArrayList<String>();
        NamedList<Object> fieldsList = getAllFacetFields(solrServer);
        for (int i = 0; i < fieldsList.size(); i++) {
            facetNames.add(fieldsList.getName(i));
        }
        return facetNames;
    }

    public static NamedList<Object> getAllFacetFields(SolrServer solrServer) throws SolrServerException,
                                                                            IOException {
        LukeRequest qr = new LukeRequest();
        NamedList<Object> qresp = solrServer.request(qr);
        Object fields = qresp.get("fields");
        if (fields instanceof NamedList<?>) {
            @SuppressWarnings("unchecked")
            NamedList<Object> fieldsList = (NamedList<Object>) fields;
            return fieldsList;
        } else {
            throw new IllegalStateException(
                    "Fields container is not a NamedList, so there is no facet information available");
        }
    }

    /**
     * This method parses the {@link Set} of {@link Constraint} and update the {@link SolrQuery} with
     * corresponding field queries. Name of the field is obtained from associated {@link Facet} of a
     * constraint and the value is obtained from the constraint itself.
     * 
     * @param constraints
     *            {@link Set} of {@link Constraint}s to be transformed into the given <code>solrQuery</code>
     * @param solrQuery
     *            {@link SolrQuery} to be updated with the given <code>constraints</code>
     */
    public static void addConstraintsToSolrQuery(Set<Constraint> constraints, SolrQuery solrQuery) {
        if (constraints != null) {
            for (Constraint constraint : constraints) {
                solrQuery.addFilterQuery(constraint.getFacet().getLabel(null) + ":" + constraint.getValue());
            }
        }
    }
}
