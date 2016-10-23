package daemon

import (
	"fmt"
	"net/http"
	"net/url"
	"time"

//	"github.com/savaki/geoip2"
	"gopkg.in/mgo.v2"
)

var api = []*Command{
	jsonCmd,
	ipLocateCmd,
}

var (
	jsonCmd = &Command{
		Path: "/json",
		GET:  getJson,
	}
	ipLocateCmd = &Command{
		Path: "/iplocate",
		GET:  getIpLocation,
	}
)

func getJson(c *Command, r *http.Request) Response {
	data, err := getData(c.d)
	if err != nil {
		fmt.Println("error retrieving data: ", err)
		return &resp{
			Status: http.StatusInternalServerError,
			Result: nil,
		}
	}

	return SyncResponse(data)
}

func getData(d *Daemon) (interface{}, error) {
	URL, err := url.Parse(d.Config.DataURL)
	if err != nil {
		return nil, fmt.Errorf("could not parse url from config: ", err)
	}
	switch URL.Scheme {
	case "mongodb":
		return getMongoData(d.Config.DataURL, d.Config.CollectionName)
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

func getIpLocation(c *Command, r *http.Request) Response {
	// do switch on a config parameter
	// and based on that either call maxmind or
	// some other service for geolocating

	geoLocator := c.d.Config.IPGeolocator["Vendor"]
	switch geoLocator {
	case "MaxMind":
		maxMindUserID := c.d.Config.IPGeolocator["MaxMindUserID"]
		maxMindLicenseKey := c.d.Config.IPGeolocator["MaxMindLicenseKey"]
		fmt.Println(maxMindUserID, maxMindLicenseKey)
        return &resp{
            Status: http.StatusNoContent,
            Result: nil,
        }

	default:
        return &resp{
            Status: http.StatusInternalServerError,
            Result: nil,
        }
	}
}
