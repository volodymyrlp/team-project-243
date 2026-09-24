import L from "leaflet";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";

import "leaflet/dist/leaflet.css";

import "./WorldMap.scss";

const markerIcon = L.icon({
  iconUrl: new URL(
    "leaflet/dist/images/marker-icon.png",
    import.meta.url
  ).href,
  iconRetinaUrl: new URL(
    "leaflet/dist/images/marker-icon-2x.png",
    import.meta.url
  ).href,
  shadowUrl: new URL(
    "leaflet/dist/images/marker-shadow.png",
    import.meta.url
  ).href,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

L.Marker.prototype.options.icon = markerIcon;

export const WorldMap = () => {
  return (
    <section className='world-map'>
      <div className='world-map__heading'>
        <span className='world-map__label'>EXPLORE THE WORLD</span>

        <h2>Where will your journey take you?</h2>

        <p>
          Discover destinations, plan your route, and keep all your travel ideas
          in one place.
        </p>
      </div>

      <div className='world-map__container'>
        <MapContainer
          center={[48.5, 15]}
          zoom={4}
          scrollWheelZoom={false}
          className='world-map__map'
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
          />

          <Marker position={[40.4168, -3.7038]}>
            <Popup>
              <strong>Madrid</strong>
              <br />
              Spain
            </Popup>
          </Marker>

          <Marker position={[41.9028, 12.4964]}>
            <Popup>
              <strong>Rome</strong>
              <br />
              Italy
            </Popup>
          </Marker>

          <Marker position={[48.2082, 16.3738]}>
            <Popup>
              <strong>Vienna</strong>
              <br />
              Austria
            </Popup>
          </Marker>

          <Marker position={[52.52, 13.405]}>
            <Popup>
              <strong>Berlin</strong>
              <br />
              Germany
            </Popup>
          </Marker>

          <Marker position={[48.8566, 2.3522]}>
            <Popup>
              <strong>Paris</strong>
              <br />
              France
            </Popup>
          </Marker>
        </MapContainer>
      </div>
    </section>
  );
};