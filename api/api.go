package api

import (
    "encoding/json"
	"fmt"
	"net"
	"net/http"
	"net/url"
	"os"
	"time"

	"github.com/jaymell/go-serve/daemon"
	"github.com/savaki/geoip2"
	"gopkg.in/mgo.v2"
)

// leaving the json itself completely untyped:
type Data interface{}

type APIConfig struct {
    DataURL        string `json: "DataURL"`
    CollectionName string `json: "CollectionName"`
    IPGeolocator map[string]string `json: "IPGeolocator"`
}

// stores persistent stuff needed for API, e.g., geolocation object, db handle
type API struct {
	config *APIConfig
	routes []*daemon.Command
	ipGeolocator IPGeolocator
}

type IPLocation struct {
	Latitude float64 `json: "latitude"`
	Longitude float64 `json: "longitude"`
	CountryCode string `json: country_iso"`
}

type IPGeolocator interface {
	IPLocation(ip string) (*IPLocation, error)
}

type MaxMindIPGeolocator struct {
	api *geoip2.Api
}

func newMaxMind(userID string, licenseKey string) (*MaxMindIPGeolocator, error) {
	gl := &MaxMindIPGeolocator{}
	// can successful creds be validated?
	gl.api = geoip2.New(userID, licenseKey)
	return gl, nil
}

func (gl *MaxMindIPGeolocator) ipLocation(ip string) (*geoip2.Response, error) {
	// FIXME: context?
	resp, err := gl.api.Insights(nil, ip)
	if err != nil {
		return nil, fmt.Errorf("Failed to get response from MaxMind")
	}

	// TODO: write response data to mongo cache:

	return &resp, nil
}

func (gl *MaxMindIPGeolocator) IPLocation(ip string) (*IPLocation, error) {

	// TODO: check IP cache before calling the 
	// api

	resp, err := gl.ipLocation(ip)
    if err != nil {
        return nil, err
    }

	// convert to vendor-generic type for returnage:
	location := IPLocation{
		Latitude: resp.Location.Latitude,
		Longitude: resp.Location.Longitude,
		CountryCode: resp.Country.IsoCode,
	}

	return &location, nil

}

func (api *API) Routes() []*daemon.Command {
	return api.routes
}

func (api *API) addRoute(cmd *daemon.Command) {
	api.routes = append(api.routes, cmd)
}

func (api *API) loadConfig(f *os.File) error {
    decoder := json.NewDecoder(f)
    config := APIConfig{}

    err := decoder.Decode(&config)
    if err != nil {
        return fmt.Errorf("unable to decode json: ", err)
    }

    api.config = &config

    return nil
}

func (api *API) loadIPGeolocator() error {

    geolocator := api.config.IPGeolocator["Vendor"]
    switch geolocator {
    case "MaxMind":
        maxMindUserID := api.config.IPGeolocator["MaxMindUserID"]
        maxMindLicenseKey := api.config.IPGeolocator["MaxMindLicenseKey"]
		maxMindIPGeolocator, err := newMaxMind(maxMindUserID, maxMindLicenseKey)
		if err != nil {
			return err
		}
		api.ipGeolocator = maxMindIPGeolocator
		return nil
    default:
		return fmt.Errorf("No useable ip geolocation vendor specified")
    }
}

func New(f *os.File) (*API, error) {

	api := &API{}

	// load config
    err := api.loadConfig(f)
    if err != nil {
        return nil, err
    }

	// initialize geolocator
	err = api.loadIPGeolocator()
    if err != nil {
        return nil, err
    }

	// connect routes with their handlers
	var (
		jsonCmd = &daemon.Command{
			Path: "/json",
			GET:  api.getJson,
		}
		ipLocateCmd = &daemon.Command{
			Path: "/iplocate",
			GET:  api.getIPLocation,
		}
	)

	var apiRoutes = []*daemon.Command{
		jsonCmd,
		ipLocateCmd,
	}

	for _, c := range apiRoutes {
		api.addRoute(c)
	}


	return api, nil

}

func (api *API) getJson(c *daemon.Command, r *http.Request) daemon.Response {
	data, err := api.getData()
	if err != nil {
		fmt.Println("error retrieving data: ", err)
		return &daemon.Resp{
			Status: http.StatusInternalServerError,
			Result: nil,
		}
	}

	return daemon.SyncResponse(data)
}

func (api *API) getData() (interface{}, error) {
	URL, err := url.Parse(api.config.DataURL)
	if err != nil {
		return nil, fmt.Errorf("could not parse url from config: ", err)
	}
	switch URL.Scheme {
	case "mongodb":
		return getMongoData(api.config.DataURL, api.config.CollectionName)
	case "http":
		return nil, fmt.Errorf("Not implemented yet")
	default:
		return nil, fmt.Errorf("Unrecognized scheme")
	}
}

func getMongoData(URL string, col string) (interface{}, error) {
	dialInfo, err := mgo.ParseURL(URL)
	if err != nil {
		return nil, fmt.Errorf("unable to parse db url: ", err)
	}

	dialInfo.Timeout = time.Duration(5) * time.Second
	session, err := mgo.DialWithInfo(dialInfo)

	if err != nil {
		return nil, fmt.Errorf("unable to connect to db: ", err)
	}
	c := session.DB(dialInfo.Database).C(col)
	results := []Data{}
	c.Find(nil).All(&results)
	// FIXME -- don't always return nil for err, probably:
	return results, nil
}

func putMongoData(URL string, col string) error {
	return nil
}

func (api *API) getIPLocation(c *daemon.Command, r *http.Request) daemon.Response {

	ip := r.URL.Query().Get("ip")
	if ip == "" {
        return &daemon.Resp{
            Status: http.StatusBadRequest,
            Result: nil,
        }
	}

	// validate an ip address was actually passed:
	validIP := net.ParseIP(ip)
	if validIP == nil {
        return &daemon.Resp{
            Status: http.StatusBadRequest,
            Result: nil,
        }
	}

	ipLocation, err := api.ipGeolocator.IPLocation(ip)
	if err != nil {
        return &daemon.Resp{
            Status: http.StatusInternalServerError,
            Result: nil,
        }

	}
	return daemon.SyncResponse(ipLocation)
}
