/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  This will turn on a switch at a certain time every day, if the moisture level is below some value.  It will then turn
 *  of the switch after a specific amount of time and check weather.  The Weather check as well as the sensor are optional.
 *
 *  Author: Andrew Dumaresq (dumaresq@gmail.com)
 */
definition(
    name: "Water Lawn",
    namespace: "dumaresq",
    author: "Andrew Dumaresq",
    description: "This will turn on your sprinkler at a given time, for a set amount of time depending on moister from a spruce sensor and the weather forcast",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
 	section("What to Turn on?") {
		input "switch1", "capability.switch"
	}
	section("Schedule") {
		input name: "startTime", title: "Turn On Time?", type: "time"
        input name: "delay", title: "How Long to water (45)?", type: "number"
	}
    section("Moisture Sensor") {
		input "sensor1", "capability.sensor", required: false
        input name: "highHumidity", title: "How Wet is too Wet (in %)?", type: "number", required: false
	}
    
    section("34689"){
		input "zipcode", "text", title: "Zipcode?", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    schedule(startTime, "startTimerCallback")    
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unschedule()
    schedule(startTime, "startTimerCallback")    
}


private isStormy(json) {
//	def STORMY = ['rain', 'snow', 'showers', 'sprinkles', 'precipitation']
//    def rain = json?.precip24Hour
 	def rain = json?.qpf[0]
	if (rain > 0.5) {
      log.debug "Rain is ${rain}"
      return true
//	def forecast = json?.forecast?.txt_forecast?.forecastday?.first()
//	log.debug "Checking Response"
//	if (forecast) {
//		def text = forecast?.fcttext?.toLowerCase()
//		if (text) {
//            log.debug "reponse is: ${text}"
//			def result = false
//			for (int i = 0; i < STORMY.size() && !result; i++) {
//				result = text.contains(STORMY[i])
//			}
//			return result
//		} else {
//			return false
//		}
	} else {
		log.debug "rain is ${rain}"
		return false
	}
}

def startTimerCallback() {
    sendNotificationEvent("${app.label}: Checking to see if we should Water the lawn")
	if ((sensor1 && highHumidity) && sensor1.currentHumidity > highHumidity) {
    	int hours = 48
   		def yesterday = new Date(now() - (/* 1000 * 60 * 60 */ 3600000 * hours).toLong())  
   		def lastHumDate = sensor1.latestState('humidity').date
   		if (lastHumDate < yesterday) {
   			log.warning "${app.label}: Please check sensor ${sensor1}, no humidity reports for ${hours}+ hours"
            sendNotificationEvent("${app.label}: Please check sensor ${sensor1}, no humidity reports for ${hours}+ hours")
        }
    	sendNotificationEvent("${app.label}: Not Watering, because ${sensor1} is at ${sensor1.currentHumidity}")
        log.debug "Not Watering, because ${sensor1} is at ${sensor1.currentHumidity} the cut off is ${highHumidity}"
        return
    }
    if (zipcode) {
        //def response = getTwcConditions(zipcode)
        def response = getTwcForecast(zipcode)
		//def response = getWeatherFeature("forecast", zipcode)
	  	if (isStormy(response)) {
    		sendNotificationEvent("${app.label}: Not Watering, the forcast calls for rain.")
            log.debug "Got Rain not Wattering"
        	return
		}
    }
    if (sensor1 && highHumidity) {
    	log.debug "The Humidity is: ${sensor1.currentHumidity} and our cut off is ${highHumidity} so we are watering."
    }
	switch1.on()
	def customDelay = 60 * delay
    sendNotificationEvent("${app.label}: All Checks passed, watering for ${delay} minutes.")
    log.debug "All Checks passed, watering for ${delay} minutes."
	runIn(customDelay, turnOffSwitch)
} 


def turnOffSwitch() {
    // Note this has recusion, but the switch should NEVER be left on this way
    log.debug "switch is ${switch1.currentSwitch}"
    if (switch1.currentSwitch == "on") {
		switch1.off()
    	//Sometimes my switch doesn't want to turn off so we do it again;
    	runIn(30, turnOffSwitch)
     }
}