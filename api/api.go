package api

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/jaymell/go-serve/daemon"
	"github.com/savaki/geoip2"
	"gopkg.in/mgo.v2"
    "gopkg.in/mgo.v2/bson"
)

// leaving the json itself completely untyped:
type Data interface{}

type APIConfig struct {
	DataURL        string            `json: "DataURL"`
	CollectionName string            `json: "CollectionName"`
	IPGeolocator   map[string]string `json: "IPGeolocator"`
	Cache          map[string]string `json: "Cache"`
}

// stores persistent stuff needed for API, e.g., geolocation object, db handle
type API struct {
	config       *APIConfig
	routes       []*daemon.Command
	ipGeolocator IPGeolocator
	cache        Cache
}

type IPLocation struct {
	IP string `json: "ip"`
	Latitude    float64 `json: "latitude"`
	Longitude   float64 `json: "longitude"`
	CountryCode string  `json: country_iso"`
	City string	`json: city"`
}

type IPGeolocator interface {
	IPLocation(ip string) (*IPLocation, error)
}

type Cache interface {
	GetCache(ip string) *IPLocation
	PutCache(ipLocation IPLocation) error
}

type CacheEntry struct {
	IPLocation
	Date time.Time `json: "date"`
}

type MongoCache struct {
	MongoSession
}

type MongoSession struct {
	session *mgo.Session
	col     string
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

	return &resp, nil
}

func (gl *MaxMindIPGeolocator) IPLocation(ip string) (*IPLocation, error) {

	resp, err := gl.ipLocation(ip)
	if err != nil {
		return nil, err
	}

	// convert to vendor-generic type for returnage:
	location := IPLocation{
		IP: ip,
		Latitude:    resp.Location.Latitude,
		Longitude:   resp.Location.Longitude,
		CountryCode: resp.Country.IsoCode,
		City: resp.City.Names["en"],
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

func getMongoSession(url string, col string) (*MongoSession, error) {

	dialInfo, err := mgo.ParseURL(url)
	if err != nil {
		return nil, fmt.Errorf("unable to parse db url: ", err)
	}

	dialInfo.Timeout = time.Duration(5) * time.Second

	session, err := mgo.DialWithInfo(dialInfo)
	if err != nil {
		return nil, fmt.Errorf("unable to connect to db: ", err)
	}

	return &MongoSession{
		session: session,
		col:     col,
	}, nil

}

func (api *API) loadCache() error {
	cache := api.config.Cache["Vendor"]
	switch strings.ToLower(cache) {
	case "mongo":
		url := api.config.Cache["URL"]
		col := api.config.Cache["Collection"]
		session, err := getMongoSession(url, col)
		if err != nil {
			return err
		}
		api.cache = session
		return nil
	}
}


func (m *MongoCache) GetCache(ip string) *IPLocation {

	// TODO: check that it's not expired (over 30 days?)

	query := bson.M{"ip": ip}
	var ipLocation *IPLocation
	m.session.DB().C(m.col).Find(query).One(&ipLocation)
	return ipLocation
}

func (m *MongoCache) PutCache(ipLocation *IPLocation) error {
	return m.session.DB().C(m.col).Insert(ipLocation)
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

	// load cache
	err = api.loadCache()
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

// FIXME -- make this get session using getMongoSession function:
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

	// validate that a valid ip address was passed:
	validIP := net.ParseIP(ip)
	if validIP == nil {
		return &daemon.Resp{
			Status: http.StatusBadRequest,
			Result: nil,
		}
	}

	var ipLocation *IPLocation

	// attempt to get from cache:
	ipLocation = api.cache.getCache(ip)
	if ipLocation == nil {
		ipLocation, err := api.ipGeolocator.IPLocation(ip)
		if err != nil {
			return &daemon.Resp{
				Status: http.StatusInternalServerError,
				Result: nil,
			}
		}
		// attempt to insert into cache
		go func() {
			_ = api.cache.PutCache(ipLocation)
		}
	}

	return daemon.SyncResponse(ipLocation)
}

