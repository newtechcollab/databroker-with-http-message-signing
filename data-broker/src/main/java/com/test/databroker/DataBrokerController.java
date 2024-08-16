package com.test.databroker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

@CrossOrigin
@RestController
public class DataBrokerController {

	private String brokerhostname = System.getenv("broker");
	private String broker = null;
	private static String clientId = "databroker";
	private MemoryPersistence persistence = new MemoryPersistence();
	MqttClient mqttClient = null;
	
	public DataBrokerController() {
		System.out.println("DataBrokerController being created !!");
		if (brokerhostname != null && !brokerhostname.equals("")) {
			System.out.println("MQTT Broker hostname set in the Environment variable : " + brokerhostname);
		}
		else {
			System.out.println("MQTT Broker hostname NOT set in the Environment variable, so using default value");
			//brokerhostname = "localhost";
			brokerhostname = "broker.hivemq.com";
		}
		broker = "tcp://" + brokerhostname + ":1883";
		System.out.println("MQTT Broker URL -> " + broker);
		try {
			System.out.println("Creating MQTT Client object");
			mqttClient = new MqttClient(broker, clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
	        connOpts.setCleanSession(true);
	        
			System.out.println("Connecting to MQTT Broker: " + broker);
	        mqttClient.connect(connOpts);
	        System.out.println("Connected to MQTT Broker !!");
		} catch (MqttException e) {
			System.out.println("Error while connecting to MQTT Broker -> " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@PostMapping("/data")
    public ResponseEntity<String> routedata(@RequestBody String body) {
		
		System.out.println("Data is " + body);
		byte[] messageData = body.getBytes();
		System.out.println("Message payload: " + messageData.toString());
		MqttMessage mqttMessage = new MqttMessage();
		mqttMessage.setPayload(messageData);
		try {
			mqttClient.publish("devicetopic", mqttMessage);
			System.out.println("Data sent successfully to MQTT Broker !!!");
		} catch (MqttException e) {
			System.out.println("Error while publishing message to MQTT Broker -> " + e.getMessage());
			e.printStackTrace();
			System.out.println("Since there was some error, let's try re-connecting to MQTT Broker !!");
			try {
				mqttClient = new MqttClient(broker, clientId, persistence);
				MqttConnectOptions connOpts = new MqttConnectOptions();
				connOpts.setCleanSession(true);
				System.out.println("Trying to re-connect to MQTT Broker: " + broker);
				mqttClient.connect(connOpts);
				System.out.println("Successfully re-connected to MQTT Broker !!");
			} catch (MqttException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}   
		}
		return ResponseEntity.ok("Processed data successfully !!");
	}
}
