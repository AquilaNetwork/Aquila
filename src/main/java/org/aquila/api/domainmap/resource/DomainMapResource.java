package org.aquila.api.domainmap.resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.aquila.arbitrary.ArbitraryDataFile.ResourceIdType;
import org.aquila.arbitrary.ArbitraryDataRenderer;
import org.aquila.arbitrary.misc.Service;
import org.aquila.settings.Settings;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import java.util.Map;


@Path("/")
@Tag(name = "Domain Map")
public class DomainMapResource {

    @Context HttpServletRequest request;
    @Context HttpServletResponse response;
    @Context ServletContext context;


    @GET
    public HttpServletResponse getIndexByDomainMap() {
        return this.getDomainMap("/");
    }

    @GET
    @Path("{path:.*}")
    public HttpServletResponse getPathByDomainMap(@PathParam("path") String inPath) {
        return this.getDomainMap(inPath);
    }

    private HttpServletResponse getDomainMap(String inPath) {
        Map<String, String> domainMap = Settings.getInstance().getSimpleDomainMap();
        if (domainMap != null && domainMap.containsKey(request.getServerName())) {
            // Build synchronously, so that we don't need to make the summary API endpoints available over
            // the domain map server. This means that there will be no loading screen, but this is potentially
            // preferred in this situation anyway (e.g. to avoid confusing search engine robots).
            return this.get(domainMap.get(request.getServerName()), ResourceIdType.NAME, Service.WEBSITE, inPath, null, "", false, false);
        }
        return ArbitraryDataRenderer.getResponse(response, 404, "Error 404: File Not Found");
    }

    private HttpServletResponse get(String resourceId, ResourceIdType resourceIdType, Service service, String inPath,
                                    String secret58, String prefix, boolean usePrefix, boolean async) {

        ArbitraryDataRenderer renderer = new ArbitraryDataRenderer(resourceId, resourceIdType, service, inPath,
                secret58, prefix, usePrefix, async, request, response, context);
        return renderer.render();
    }

}
