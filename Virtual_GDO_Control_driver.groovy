/** Virtual GDO Control
 *
 */

metadata {
	definition (name: "Virtual GDO Control", namespace: "maddigan", author: "Steve Maddigan") {
		capability "GarageDoorControl"
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
     if (device.currentValue("door") != "closed") {
        sendEvent(name: "door", value: "closing")
    }
}

def open() {
    log.debug "open()"
    if (device.currentValue("door") != "open") {
        sendEvent(name: "door", value: "opening")
    }
}

