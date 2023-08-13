/** Virtual GDO Control
 *
 *  Changelog:
 *    20230813 v.2.0.1 : changed current value checks from closed/opened to closing & opening
 *    20210910 v.2.0.0 : Copied from Zooz Garage Door Opener by Kevin LaFramboise (@krlaframboise) for Zooz
 *                     : Modified to support seperate Open & Close relays
*/

metadata {
	definition (name: "Virtual GDO Control", namespace: "maddigan", author: "Steve Maddigan") {
		capability "GarageDoorControl"
	}

    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
	}
}

def installed() {
    log.debug "installed"
    sendEvent(name: "door", value: "unknown")
}

def configure() {
    log.debug "configure"
}

def updated() {
	log.debug "updated"
}

def close() {
	log.debug "close()"
     if (device.currentValue("door") != "closing") {
         log.info "GDO closing"
         sendEvent(name: "door", value: "closing")
    }
}

def open() {
    log.debug "open()"
    if (device.currentValue("door") != "opening") {
        log.info "GDO opening"
        sendEvent(name: "door", value: "opening")
    }
}


