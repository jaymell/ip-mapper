package api

import (
    "encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"os"
	"time"

	"github.com/jaymell/go-serve/daemon"
//	"github.com/savaki/geoip2"
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
	Config *APIConfig
	routes []*daemon.Command
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

    api.Config = &config

    return nil
}

func New(f *os.File) (*API, error) {

	api := &API{}

    err := api.loadConfig(f)
    if err != nil {
        return nil, err
    }

	var (
		jsonCmd = &daemon.Command{
			Path: "/json",
			GET:  api.getJson,
		}
		ipLocateCmd = &daemon.Command{
			Path: "/iplocate",
			GET:  api.getIpLocation,
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
	URL, err := url.Parse(api.Config.DataURL)
	if err != nil {
		return nil, fmt.Errorf("could not parse url from config: ", err)
	}
	switch URL.Scheme {
	case "mongodb":
		return getMongoData(api.Config.DataURL, api.Config.CollectionName)
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

//func getGeoIP2Response(ip string) (MaxMindResult, error) {

//}

func (api *API) getIpLocation(c *daemon.Command, r *http.Request) daemon.Response {
	// do switch on a config parameter
	// and based on that either call maxmind or
	// some other service for geolocating

	geoLocator := api.Config.IPGeolocator["Vendor"]
	switch geoLocator {
	case "MaxMind":
		maxMindUserID := api.Config.IPGeolocator["MaxMindUserID"]
		maxMindLicenseKey := api.Config.IPGeolocator["MaxMindLicenseKey"]
		fmt.Println(maxMindUserID, maxMindLicenseKey)
        return &daemon.Resp{
            Status: http.StatusNoContent,
            Result: nil,
        }

	default:
        return &daemon.Resp{
            Status: http.StatusInternalServerError,
            Result: nil,
        }
	}
}
