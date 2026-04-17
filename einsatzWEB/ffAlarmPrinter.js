// decode an encoded string
// src: https://valhalla.github.io/demos/polyline/decode.js
function decode(encoded) {
    //precision
    const inv = 1.0 / 1e5;
    const decoded = [];
    const previous = [0, 0];
    let i = 0;
    //for each byte
    while(i < encoded.length) {
        //for each coord (lat, lon)
        const ll = [0, 0];
        for(let j = 0; j < 2; j++) {
            let shift = 0;
            let byte = 0x20;
            //keep decoding bytes until you have this coord
            while(byte >= 0x20) {
                byte = encoded.charCodeAt(i++) - 63;
                ll[j] |= (byte & 0x1f) << shift;
                shift += 5;
            }
            //add previous offset to get final value and remember for next one
            ll[j] = previous[j] + (ll[j] & 1 ? ~(ll[j] >> 1) : (ll[j] >> 1));
            previous[j] = ll[j];
        }
        //scale by precision and chop off long coords
        decoded.push([ll[0] * inv,ll[1] * inv]);
    }
    //hand back the list of coordinates
    return decoded;
}

function initialize() {

    let mapZoom = 13;
    if (
        routeJson.hasOwnProperty("routes")
        && routeJson.routes.hasOwnProperty(0)
        && routeJson.routes[0].hasOwnProperty("distance")
        && routeJson.routes[0].hasOwnProperty("duration")
    ) {
        const totalDistance = routeJson.routes[0].distance;
        let totalDuration = routeJson.routes[0].duration;

        if (totalDistance > 0 && totalDistance < 5000) {
            mapZoom = 16;
        } else if (totalDistance < 10000) {
            mapZoom = 15;
        } else if (totalDistance < 20000) {
            mapZoom = 14;
        }

        const anfahrt = document.getElementById("einsatz-anfahrt-data");
        anfahrt.innerHTML  = (Math.round(totalDistance / 10) / 100).toString();
        anfahrt.innerHTML += "km - ungef&auml;hr ";
        if (totalDuration > 3600) {
            const hours = Math.floor(totalDuration / 3600);
            totalDuration = hours % 3600;
            anfahrt.innerHTML += hours + " Stunden ";
        }
        if (totalDuration > 60) {
            const mins = Math.round(totalDuration / 60);
            anfahrt.innerHTML += mins + " Minuten";
        } else {
            anfahrt.innerHTML += "1 Minute";
        }

        if (routeSteps !== null) {
            let routeDescription = "<h2>Routenbeschreibung</h2><ul>";
            for (let i = 0; i < routeSteps.length; i++) {
                routeDescription += "<li>" + routeSteps[i].text;
                if (i < (routeSteps.length - 1)) {
                    routeDescription += " (" + routeSteps[i].distance + "m)";
                }
                routeDescription += "</li>";
            }
            routeDescription += "</ul>";
            document.getElementById("directionsPanel").innerHTML = routeDescription;
        }

        if (totalDistance >= 10000) {
            document.getElementById("directionsPanel").style.display = "block";
            document.getElementById("directionsPanel").style.pageBreakBefore = "always";
        }
    }

    const smallScreenBreakpoint = 1024;
    const mobileMapHeightRatio = 0.5;
    const mapElement = document.getElementById("map");
    const getCurrentWindowWidth = () => window.innerWidth || document.documentElement.clientWidth;
    const getFullscreenElement = () => document.fullscreenElement || document.webkitFullscreenElement;
    const isMapFullscreen = () => getFullscreenElement() === mapElement;
    const requestMapFullscreen = () => {
        if (mapElement.requestFullscreen) {
            return mapElement.requestFullscreen();
        }

        if (mapElement.webkitRequestFullscreen) {
            return mapElement.webkitRequestFullscreen();
        }

        return null;
    };
    const exitMapFullscreen = () => {
        if (document.exitFullscreen) {
            return document.exitFullscreen();
        }

        if (document.webkitExitFullscreen) {
            return document.webkitExitFullscreen();
        }

        return null;
    };
    const getCurrentWindowHeight = () => {
        if (window.visualViewport && window.visualViewport.height) {
            return window.visualViewport.height;
        }

        return window.innerHeight || document.documentElement.clientHeight;
    };
    const isSmallScreen = () => getCurrentWindowWidth() <= smallScreenBreakpoint;
    const getMapHeight = () => {
        const currentWindowHeight = getCurrentWindowHeight();

        if (isMapFullscreen()) {
            return currentWindowHeight;
        }

        if (isSmallScreen()) {
            return Math.max(320, Math.round(currentWindowHeight * mobileMapHeightRatio));
        }

        return Math.max(400, Math.round(getCurrentWindowWidth() * 0.5));
    };
    const getHydrantPopupContent = (hydrant) => (hydrant.text || "").trim().replace(/(<br\s*\/?>\s*)+$/i, "");
    const getHydrantArtHeading = (hydrantPopupContent) => {
        const artMatch = hydrantPopupContent.match(/Art:\s*([^<]+)/i);
        return artMatch ? artMatch[1].trim() : "";
    };
    const getHydrantPopupHeading = (hydrantPopupContent) => getHydrantArtHeading(hydrantPopupContent)
        || (hydrantPopupContent.startsWith("Ansaugplatz") ? "Ansaugplatz" : "Hydrant");
    const getHydrantPopupDetails = (hydrantPopupContent) => {
        if (getHydrantArtHeading(hydrantPopupContent) === "") {
            return hydrantPopupContent;
        }

        return hydrantPopupContent
            .replace(/(?:^|<br\s*\/?>\s*)Art:\s*[^<]+(?:<br\s*\/?>\s*)?/gi, "<br />")
            .replace(/^(?:<br\s*\/?>\s*)+/i, "")
            .replace(/(?:<br\s*\/?>\s*)+$/i, "")
            .replace(/(<br\s*\/?>\s*){2,}/gi, "<br />");
    };
    const getHydrantPopupHtml = (hydrant) => {
        const hydrantPopupContent = getHydrantPopupContent(hydrant);
        const hydrantPopupHeading = getHydrantPopupHeading(hydrantPopupContent);
        const hydrantPopupDetails = getHydrantPopupDetails(hydrantPopupContent);

        if (hydrantPopupContent === "") {
            return "";
        }

        return "<div class=\"hydrant-popup\">"
            + "<div class=\"hydrant-popup-heading\">" + hydrantPopupHeading + "</div>"
            + (hydrantPopupDetails === "" ? "" : "<div class=\"hydrant-popup-details\">" + hydrantPopupDetails + "</div>")
            + "</div>";
    };
    const getHydrantTitleText = (hydrant) => getHydrantPopupContent(hydrant).replaceAll("<br />", "\n").replace(/<br\s*\/?>/g, "\n");

    mapElement.style.height = getMapHeight() + "px";

    const map = L.map('map').setView([endLat, endLong], mapZoom);
    const osm = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 20,
        attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributers'
    }).addTo(map);
    const BasemapAT_highdpi = L.tileLayer('https://mapsneu.wien.gv.at/basemap/bmaphidpi/{type}/google3857/{z}/{y}/{x}.{format}', {
        maxZoom: 19,
        attribution: 'Datenquelle: <a href="https://www.basemap.at">basemap.at</a>',
        type: 'normal',
        format: 'jpeg',
        bounds: [[46.35877, 8.782379], [49.037872, 17.189532]]
    });
    const BasemapAT_orthofoto = L.tileLayer('https://mapsneu.wien.gv.at/basemap/bmaporthofoto30cm/{type}/google3857/{z}/{y}/{x}.{format}', {
        maxZoom: 20,
        attribution: 'Datenquelle: <a href="https://www.basemap.at">basemap.at</a>',
        type: 'normal',
        format: 'jpeg',
        bounds: [[46.35877, 8.782379], [49.037872, 17.189532]]
    });

    const bevMap = L.tileLayer("https://maps.bev.gv.at/tiles/karte/{z}/{x}/{y}.png", {
        minZoom: 8,
        maxNativeZoom: 16,
        maxZoom: 20,
        type: 'normal',
        format: 'jpeg',
        attribution: 'Map data: &copy; <a href="https://maps.bev.gv.at/">Austrian Map BEV CC BY 4.0</a>'
    });

    const OpenFireMap = L.tileLayer('http://openfiremap.org/hytiles/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: 'Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors | Map style: &copy; <a href="http://www.openfiremap.org">OpenFireMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)'
    }).addTo(map);
    const BasemapAT_overlay = L.tileLayer('https://mapsneu.wien.gv.at/basemap/bmapoverlay/{type}/google3857/{z}/{y}/{x}.{format}', {
        maxZoom: 19,
        attribution: 'Datenquelle: <a href="https://www.basemap.at">basemap.at</a>',
        type: 'normal',
        format: 'png',
        bounds: [[46.35877, 8.782379], [49.037872, 17.189532]]
    });

    const basemaps = {
        'OpenStreetMap': osm,
        'BasemapAT': BasemapAT_highdpi,
        'BasemapAT Orthofoto': BasemapAT_orthofoto,
        "Austrian Map / ÖK": bevMap,
    };

    const overlays = {
        OpenFireMap: OpenFireMap,
        BasemapAT: BasemapAT_overlay
    };

    let isLayerControlCollapsed = isSmallScreen();
    let layerControl = L.control.layers(basemaps, overlays, {collapsed: isLayerControlCollapsed}).addTo(map);
    let fullscreenToggleLink;

    const updateFullscreenToggleState = () => {
        if (!fullscreenToggleLink) {
            return;
        }

        const isFullscreen = isMapFullscreen();
        fullscreenToggleLink.innerHTML = isFullscreen ? "&times;" : "&#x26F6;";
        fullscreenToggleLink.title = isFullscreen ? "Vollbild beenden" : "Karte im Vollbild anzeigen";
        fullscreenToggleLink.setAttribute("aria-label", fullscreenToggleLink.title);
    };

    const FullscreenControl = L.Control.extend({
        options: {
            position: "topright"
        },

        onAdd() {
            const container = L.DomUtil.create("div", "leaflet-bar leaflet-control leaflet-control-custom leaflet-control-fullscreen");
            fullscreenToggleLink = L.DomUtil.create("a", "leaflet-control-fullscreen-toggle", container);
            fullscreenToggleLink.href = "#";
            updateFullscreenToggleState();

            L.DomEvent.disableClickPropagation(container);
            L.DomEvent.on(fullscreenToggleLink, "click", (event) => {
                L.DomEvent.stop(event);

                if (isMapFullscreen()) {
                    exitMapFullscreen();
                } else {
                    requestMapFullscreen();
                }
            });

            return container;
        }
    });

    new FullscreenControl().addTo(map);

    const updateResponsiveMapLayout = () => {
        mapElement.style.height = getMapHeight() + "px";

        const shouldBeCollapsed = isSmallScreen();
        if (shouldBeCollapsed !== isLayerControlCollapsed) {
            map.removeControl(layerControl);
            isLayerControlCollapsed = shouldBeCollapsed;
            layerControl = L.control.layers(basemaps, overlays, {collapsed: isLayerControlCollapsed}).addTo(map);
        }

        updateFullscreenToggleState();
        map.invalidateSize();
    };

    let resizeTimer;
    const queueResponsiveMapUpdate = () => {
        window.clearTimeout(resizeTimer);
        resizeTimer = window.setTimeout(() => {
            updateResponsiveMapLayout();
        }, 150);
    };

    window.addEventListener("resize", queueResponsiveMapUpdate);
    if (window.visualViewport) {
        window.visualViewport.addEventListener("resize", queueResponsiveMapUpdate);
    }
    document.addEventListener("fullscreenchange", updateResponsiveMapLayout);
    document.addEventListener("webkitfullscreenchange", updateResponsiveMapLayout);

    // Add hydrants to map first, so other layers are infront
    for (const hydrant of hydrantsJson) {
        console.log(hydrant);
        const hydrantPopupHtml = getHydrantPopupHtml(hydrant);
        const hydrantMarker = new L.Marker(
            [hydrant.Lat, hydrant.Lon],
            {
                title: getHydrantTitleText(hydrant)
            }
        ).addTo(map);

        if (hydrantPopupHtml !== "") {
            hydrantMarker.bindPopup(hydrantPopupHtml);
        }

        // https://stackoverflow.com/a/61982880
        hydrantMarker._icon.classList.add("hydrantHue");
    }

    // Add Route geometry to map
    if (
        routeJson.hasOwnProperty("routes")
        && routeJson.routes.hasOwnProperty(0)
        && routeJson.routes[0].hasOwnProperty("geometry")
    ) {
        console.log("geometry: " + routeJson.routes[0].geometry);
        const routeLine = L.polyline(decode(routeJson.routes[0].geometry), {
            color: "blue"
        }).addTo(map);
    }


    // Add Start Pin
    const startMarker = new L.Marker(
        [startLat, startLong],
        {
            title: "Feuerwehr Pitten"
        }
    ).addTo(map);
    startMarker._icon.classList.add("startHue");

    // Add End Pin
    const endMarker = new L.Marker(
        [endLat, endLong],
        {
            title: "Einsatzort"
        }
    ).addTo(map);
    // https://stackoverflow.com/a/61982880
    endMarker._icon.classList.add("endHue");

    L.control.scale({
        imperial: false
    }).addTo(map);

    updateResponsiveMapLayout();
}
