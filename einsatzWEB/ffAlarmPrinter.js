
// src: https://valhalla.github.io/demos/polyline/decode.js
//decode an encoded string
function decode(encoded) {
    //precision
    var inv = 1.0 / 1e5;
    var decoded = [];
    var previous = [0,0];
    var i = 0;
    //for each byte
    while(i < encoded.length) {
        //for each coord (lat, lon)
        var ll = [0,0]
        for(var j = 0; j < 2; j++) {
            var shift = 0;
            var byte = 0x20;
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
        anfahrt.innerHTML  = Math.round(totalDistance / 10) / 100;
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
    const googleStreets = L.tileLayer('https://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}',{
        maxZoom: 20,
        subdomains:['mt0','mt1','mt2','mt3']
    });
    const googleSat = L.tileLayer('https://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',{
        maxZoom: 20,
        subdomains:['mt0','mt1','mt2','mt3']
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
        'Google Streets': googleStreets,
        'BasemapAT Orthofoto': BasemapAT_orthofoto,
        'Google Sat': googleSat
    };

    const overlays = {
        OpenFireMap: OpenFireMap,
        BasemapAT: BasemapAT_overlay
    };

    const layerControl = L.control.layers(basemaps, overlays, {collapsed: false}).addTo(map);

    // Add hydrants to map first, so other layers are infront
    for (const i in hydrantsJson) {
        console.log(hydrantsJson[i]);
        const hydrantMarker = new L.Marker(
            [hydrantsJson[i].Lat, hydrantsJson[i].Lon],
            {
                title: hydrantsJson[i].text.replaceAll("<br />", "\n")
            }
        ).addTo(map);

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

}