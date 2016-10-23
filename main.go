package main

import (
	"fmt"
	"./daemon"
	"os"
)

func run() error {
	var d daemon.Daemon
	err := d.Init()
	if err != nil {
		return fmt.Errorf("failed on daemon init: ", err)
	}
	d.Start()
	return nil
}

func main() {
	fmt.Println("Starting server... ")
	err := run()
	if err != nil {
		fmt.Println("failed: ", err)
		os.Exit(1)
	}
}
