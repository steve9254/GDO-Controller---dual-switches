/* Garage Door Controller - Dual Switch v.2.0.5
 *
 *  Changelog:
 *    20221028 v.2.0.5 : changed all logTrace to logDebug
 *    20221027 v.2.0.4 : added flashGarageLight(), errorIndicator output & confirmClosed()
 *    20221026 v.2.0.3 : Connected new GDO control driver
 *    20221025 v.2.0.2 : Removed parent/child app setup & disconnected zooz driver
 *    20221024 v.2.0.1 : Added support for open & closed contact sensors
 *    20210910 v.2.0.0 : Copied from Zooz Garage Door Opener App v1.2 (Apps Code) by Kevin LaFramboise (@krlaframboise) for Zooz
 *                     : Modified to support seperate Open & Close relays
 */

definition(
    name        : "Garage Door Controller - Dual Switch (v.2.0.5)",
    namespace   : "maddigan",
    author      : "Steve Maddigan",
    description : "Garage door controller with seperate OPEN/CLOSE buttons",
    category    : "Convenience",
    iconUrl     : "",
    iconX2Url   : "",
    importUrl   : "https://raw.githubusercontent.com/steve9254/GDO-Controller---dual-switches/main/GDO_Controller-Dual_Switch_app.groovy"
)

preferences {
    page(name: "mainPage", title: "<h2>Garage Door Controller - Dual Switch (v.2.0.5)</h2>", install: true, uninstall: true) {
	    section("<b>Controls</b>") {

            input name: "garageControl",       type: "capability.garageDoorControl",      title: "Garage Door Control",
		        description: "Use a Virtual Garage Door Control device",                   multiple: false, required: true

            input name: "closeRelaySwitch",    type: "capability.switch",                 title:"Close Relay Switch",
		        description: "Physical relay switch that closes garage door",             multiple: false, required: true

            input name: "openRelaySwitch",     type: "capability.switch",                 title: "Open Relay Switch",
		        description: "Physical relay switch that opens your garage door",          multiple: false, required: true

            input name: "lightRelaySwitch",    type: "capability.switch", 		            title: "Light Relay Switch",
                description: "Relay switch that toggles the garage door light",        multiple: false, required: false
        }

        section("<b>Contacts</b>") {
            input name: "closedSensor",        type: "capability.contactSensor",          title: "Garage Fully Closed Sensor",
                description: "Sensor that indicates when GD is fully closed",          multiple: false, required: true

            input name: "openedSensor",        type: "capability.contactSensor",          title: "Garage Fully Open Contact",
                description: "Sensor that indicates when GD is fully open",            multiple: false, required: true

            input name: "movingSensor",        type: "capability.accelerationSensor",     title: "Garage Acceleration Sensor",
                description: "Acceleration sensor that indicates GD is moving",        multiple: false, required: true
        }

        section("<b>Indicators</b>") {
            input name: "inProgressIndicator", type: "capability.switch",                 title: "In Progress Indicator",
                description: "Switch that indicates that the opener is active",        multiple: false, required: false
            input name: "errorIndicator",      type: "capability.switch",                 title: "Error Indicator",
                description: "Switch that indicates that there is a problem",          multiple: false, required: false
        }

        section{
            input name: "enableLogging", type: "bool", title: "Enable Debug Logging?", defaultValue: false, required: true
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    log.warn "Updated() ..."

    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    log.warn "Initialized() ..."
    setupSubscriptions()
}

def setupSubscriptions() {
    subscribe(garageControl,    "door",           GarageControlHandler)
    subscribe(closeRelaySwitch, "switch.on",      closeRelaySwitchHandler)
    subscribe(openRelaySwitch,  "switch.on",      openRelaySwitchHandler)
    subscribe(movingSensor,     "acceleration",   inProgressHandler)
    subscribe(closedSensor,     "contact.closed", inProgressHandler)
    subscribe(openedSensor,     "contact.closed", inProgressHandler)
}

def GarageControlHandler(evt) {
    logDebug "GarageControlhandler() ...${evt.name} ${evt.value}"

    if ( evt.value == "closing" ) {
        String ClosedStatus = closedSensor?.currentValue("contact")
        if ( ClosedStatus != "closed" ) {
            flashGarageLight()
            activateCloseRelaySwitch()
            runIn(30, confirmClosed)
        }
    } else if ( evt.value == "opening" ) {
            activateOpenRelaySwitch()
    }
}

def activateCloseRelaySwitch() {
    logDebug "activateCloseRelaySwitch() ..."
    closeRelaySwitch?.on()
}

def activateOpenRelaySwitch() {
    logDebug "activateOpenRelaySwitch() ..."
    openRelaySwitch?.on()
}

def closeRelaySwitchHandler(evt) {
    logDebug "closeRelaySwitchHandler() ..."
    String closedStatus = closedSensor?.currentValue("contact")

    if ( closedStatus != "closed" ) {
        sendDoorEvent("closing")
    } else {
        sendDoorEvent("closed")
    }
}

def openRelaySwitchHandler(evt) {
    logDebug "openRelaySwitchHandler() ..."
    unschedule(confirmClosed)
    String openedStatus = openedSensor?.currentValue("contact")

    if ( openedStatus != "closed" ) {
        sendDoorEvent("opening")
    } else {
        sendDoorEvent("open")
    }
}

def inProgressHandler(evt) {
    logDebug "inProgressHandler() ..."
    String doorStatus = garageControl?.currentValue("door")
    String movingStatus = movingSensor?.currentValue("acceleration")
    String openedStatus = openedSensor?.currentValue("contact")
    String closedStatus = closedSensor?.currentValue("contact")
    logDebug "movingStatus=${movingStatus}, closedSensor=${closedStatus}, openSensor=${openedStatus}"

    if ( errorIndicator?.currentValue("switch") == "on" ) {
        logDebug "Clearing errorIndicator"
        errorIndicator?.off()
    }

    if (( movingStatus == "active" ) && ( closedStatus == "open" ) && ( openedStatus == "open" )) {
        logDebug "Setting inProgressIndicator"
        inProgressIndicator?.on()
    } else {
        logDebug "Clearing inProgressIndicator"
        inProgressIndicator?.off()
    }

    updateDoorStatus()
}

def updateDoorStatus() {
    logDebug "updateDoorStatus() ..."
    String movingStatus = movingSensor?.currentValue("acceleration")
    String closedStatus = closedSensor?.currentValue("contact")

    if ( movingStatus == "inactive" ) {
        if ( closedStatus == "closed" ) {
        	sendDoorEvent("closed")
       	} else {
            sendDoorEvent("open")
        }
    }
}

def sendDoorEvent(value) {
    logDebug "sendDoorEvent() ..."
    garageControl.sendEvent(name: "door", value: value)
}

def confirmClosed() {
    logDebug "confirmClosed() ..."
    if ( closedSensor?.currentValue("contact") != "closed" ) {
        errorIndicator?.on()
    }
}

def flashGarageLight() {
    if (lightRelaySwitch) {
        for ( i in 0..3 ) {
            lightRelaySwitch?.on()
            pauseExecution(1000)
            lightRelaySwitch?.off()
            pauseExecution(1000)
        }
    }
}

def logDebug(String msg) {
    if (debugLogging != false) {
        log.debug "$msg"
    }
}

def logTrace(String msg) {
     log.trace "$msg"
}
