/**
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  2017 
 */

metadata {
    definition(name: "Xiaomi Curtain & Blind SJ ", namespace: "ShinJjang", author: "ShinJjang", ocfDeviceType: "oic.d.blind") {
      capability "Window Shade" 
      capability "Switch Level"
      capability "Switch"
      capability "Actuator"
      capability "Health Check"
      capability "Sensor"
      capability "Refresh"
      
    command "levelOpenClose"
    command "shadeAction"
    command "Pause"       

        fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0004, 0003, 0005, 000A, 0102, 000D, 0013, 0006, 0001, 0406", outClusters: "0019, 000A, 000D, 0102, 0013, 0006, 0001, 0406", manufacturer: "LUMI", model: "lumi.curtain", deviceJoinName: "Xiaomi Curtain V1"
//        fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0003, 0102, 000D, 0013, 0001", outClusters: "0003, 000A", manufacturer: "LUMI", model: "lumi.curtain.hagl04", deviceJoinName: "Xiaomi Curtain V2"
        fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0004, 0003, 0005, 000A, 0102, 000D, 0013", outClusters: "000A", manufacturer: "LUMI", model: "lumi.curtain.aq2", deviceJoinName: "Xiaomi Blind"

    }


    
    preferences {
          input name: "mode", type: "bool", title: "Xiaomi Curtain Direction Set", description: "Reverse Mode ON", required: true,
             displayDuringSetup: true
//          input name: "openInt", type: "integer", title: "Open Percetage(**% 열림)", defaultValue: 30, description: "**% 열림을 설정합니다.", required: true
   }    

    tiles(scale: 2) {
        multiAttributeTile(name: "windowShade", type: "generic", width: 6, height: 4) {
            tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState("closed", label: 'closed', action: "windowShade.open", icon: "st.doors.garage.garage-closed", backgroundColor: "#A8A8C6", nextState: "opening")
                attributeState("open", label: 'open', action: "windowShade.close", icon: "st.doors.garage.garage-open", backgroundColor: "#F7D73E", nextState: "closing")
                attributeState("closing", label: '${name}', action: "windowShade.open", icon: "st.contact.contact.closed", backgroundColor: "#B9C6A8")
                attributeState("opening", label: '${name}', action: "windowShade.close", icon: "st.contact.contact.open", backgroundColor: "#D4CF14")
                attributeState("partially open", label: 'partially\nopen', action: "windowShade.close", icon: "st.doors.garage.garage-closing", backgroundColor: "#D4ACEE", nextState: "closing")
            }
            tileAttribute("device.level", key: "SLIDER_CONTROL") {
                attributeState("level", action: "setLevel")
            }
        }
        standardTile("open", "open", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("open", label: 'open', action: "windowShade.open", icon: "st.contact.contact.open")
        }
        standardTile("close", "close", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("close", label: 'close', action: "windowShade.close", icon: "st.contact.contact.closed")
        }
        standardTile("stop", "stop", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("stop", label: 'stop', action: "Pause", icon: "st.illuminance.illuminance.dark")
        }
        standardTile("refresh", "command.refresh", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: " ", action: "refresh.refresh", icon: "https://www.shareicon.net/data/128x128/2016/06/27/623885_home_256x256.png"
        }
        standardTile("home", "device.level", width: 2, height: 2, decoration: "flat") {
            state "default", label: "home", action:"presetPosition", icon:"st.Home.home2"
        }

        main(["windowShade"])
        details(["windowShade", "open", "stop", "close", "refresh"])
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
    def parseMap = zigbee.parseDescriptionAsMap(description)
//            log.debug "parseMap11:${parseMap}"    
    def event = zigbee.getEvent(description)

    try {
            def windowShadeStatus = ""
            def curtainLevel = null

         if (parseMap["cluster"] == "000D" && parseMap["attrId"] == "0055") {
         	if(parseMap.raw.endsWith("00000000") || parseMap["result"] == "success") {
                long theValue = Long.parseLong(parseMap["value"], 16)
                float floatValue = Float.intBitsToFloat(theValue.intValue());
//                 log.debug "long => ${theValue}, float => ${floatValue}"
            	 curtainLevel = floatValue.intValue()
                 log.debug "Level => ${curtainLevel}"
			} else {
	            log.debug "running…"
            }
        } else if (parseMap["cluster"] == "0102" && parseMap["attrId"] == "0008") {
                long endValue = Long.parseLong(parseMap["value"], 16)
                 curtainLevel = endValue
                 log.debug "endLevel=>${curtainLevel}"
        } else if (parseMap["clusterId"] == "0000" && parseMap["encoding"] == "42") {
         			def valueData = parseMap["value"]
                    def eventStack = []
                    def position = valueData[36,37];
                    String hexposition = position
	                long endValue = Long.parseLong(hexposition, 16)
    	             curtainLevel = endValue
                    log.debug "check position = ${position}, check Level = ${curtainLevel}"
        } else if (parseMap.raw.startsWith("0104")) {
            log.debug "Xiaomi Curtain"
        } else if (parseMap.raw.endsWith("0007")) {
            log.debug "running…"
        }
        else {
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
        }
        	if(curtainLevel >= 0){
                if(mode == true) {
                    if (curtainLevel == 100) {
                        log.debug "Just Closed"
                        windowShadeStatus = "closed"
                        curtainLevel = 0
                    } else if (curtainLevel == 0) {
                        log.debug "Just Fully Open"
                        windowShadeStatus = "open"
                        curtainLevel = 100
                    } else {
                        log.debug curtainLevel + '% Partially Open'
                        windowShadeStatus = "partially open"
                        curtainLevel = 100 - curtainLevel
                    }
                } else {
                    if (curtainLevel == 100) {
                        log.debug "Just Fully Open"
                        windowShadeStatus = "open"
                        curtainLevel = 100
                    } else if (curtainLevel > 0) {
                        log.debug curtainLevel + '% Partially Open'
                        windowShadeStatus = "partially open"
                        curtainLevel = curtainLevel
                    } else {
                        log.debug "Just Closed"
                        windowShadeStatus = "closed"
                        curtainLevel = 0
                    }
                }
				def eventStack = []
                eventStack.push(createEvent(name:"windowShade", value: windowShadeStatus as String))
                eventStack.push(createEvent(name:"level", value: curtainLevel))
                eventStack.push(createEvent(name:"switch", value: (windowShadeStatus == "closed" ? "off" : "on")))
                return eventStack                
			}                
    } catch (Exception e) {
        log.warn e
    }
}

def updated() {
	sendEvent(name: "openlevel", value: openInt)
}	

def on() {
	open()
}


def off() {
	close()
}

def close() {
    log.debug "close()"
	if(mode == true){
		zigbee.command(0x0102, 0x00)
	} else {
		zigbee.command(0x0102, 0x01)
	}
}

def open() {
    log.debug "open()"
	if(mode == true){
		zigbee.command(0x0102, 0x01)
	} else {
		zigbee.command(0x0102, 0x00)
	}
}


def Pause() {
    log.debug "stop()"
   zigbee.command(0x0102, 0x02)
}

def setLevel(level) {
    if (level == null) {level = 0}
    level = level as int
    
   if(mode == true){
       if(level == 100) {
            log.debug "Set Close"
            zigbee.command(0x0102, 0x00)
        } else if(level < 1) {
           log.debug "Set Open"
              zigbee.command(0x0102, 0x01)
        } else {
           log.debug "Set Level: ${level}%"
            def f = 100 - level
           String hex = Integer.toHexString(Float.floatToIntBits(f)).toUpperCase()
           zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
       }  
    } else{
       if (level == 100){
            log.debug "Set Open"
            zigbee.command(0x0102, 0x00)
        } else if(level > 0) {
            log.debug "Set Level: ${level}%"
            String hex = Integer.toHexString(Float.floatToIntBits(level)).toUpperCase()
            zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
        } else {
            log.debug "Set Close"
            zigbee.command(0x0102, 0x01)
        } 
    }
}

def shadeAction(level) {
   if(mode == true){
       if(level == 100) {
            log.debug "Set Close"
            zigbee.command(0x0102, 0x00)
        } else if(level < 1) {
           log.debug "Set Open"
              zigbee.command(0x0102, 0x01)
        } else {
           log.debug "Set Level: ${level}%"
            def f = 100 - level
           String hex = Integer.toHexString(Float.floatToIntBits(f)).toUpperCase()
           zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
       }  
    } else{
       if (level == 100){
            log.debug "Set Open"
            zigbee.command(0x0102, 0x00)
        } else if(level > 0) {
            log.debug "Set Level: ${level}%"
            String hex = Integer.toHexString(Float.floatToIntBits(level)).toUpperCase()
            zigbee.writeAttribute(0x000d, 0x0055, 0x39, hex)
        } else {
            log.debug "Set Close"
            zigbee.command(0x0102, 0x01)
        } 
    }
}
def refresh() {
    log.debug "refresh()"
//    "st rattr 0x${device.deviceNetworkId} ${1} 0x000d 0x0055"
     zigbee.readAttribute(0x000d, 0x0055)
     }
