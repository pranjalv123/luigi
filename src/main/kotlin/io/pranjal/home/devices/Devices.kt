package io.pranjal.home.devices

import io.github.oshai.kotlinlogging.KotlinLogging
import io.pranjal.home.devices.ZigbeeDevice.Definition
import io.pranjal.home.devices.ZigbeeDevice.Definition.Location.KITCHEN
import io.pranjal.home.devices.ZigbeeDevice.Definition.Location.MASTER_BATHROOM
import io.pranjal.home.devices.makes.HueBulb
import io.pranjal.home.devices.makes.HueDimmer
import io.pranjal.home.devices.makes.InovelliSwitch
import io.pranjal.mqtt.MqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

class Devices(private val mqttClient: MqttClient) {
    object Definitions {
        val kitchenLights = listOf(
            "0x001788010d87d7cb",
            "0x001788010d87da3d",
            "0x001788010d87da56",
            "0x001788010db5e92c",
            "0x001788010db5e591",
            "0x001788010db5e673"
        ).mapIndexed { i, id -> HueBulb.Definition(id, "kitchen_light_${i}", KITCHEN) }

        val diningRoomLights = listOf<String>(
            "0x001788010db60df9",
            "0x001788010d87da29",
            "0x001788010db5e59d",
            "0x001788010db60991",
            "0x001788010db5e813",
            "0x001788010db60d15"
        ).mapIndexed { i, id ->
            HueBulb.Definition(
                id,
                "dining_room_light_${i}",
                Definition.Location.DINING_ROOM
            )
        }

        val livingRoomLights = listOf(
            "0x001788010db5e524",
            "0x001788010db5e597",
            "0x001788010db60d50",
            "0x001788010d87d9f0",
            "0x001788010d87d98f",
            "0x001788010db5e8fd",
        ).mapIndexed { i, id ->
            HueBulb.Definition(
                id,
                "living_room_light_${i}",
                Definition.Location.LIVING_ROOM
            )
        }

        val walkinClosetLights = listOf(
            "0x001788010db616c0",
            "0x001788010db6097f"
        ).mapIndexed { i, id -> HueBulb.Definition(id, "walk_in_closet_light_${i}", MASTER_BATHROOM) }

        val masterBedroomLights = listOf(
            "0x001788010db5e5b2",
            "0x001788010db60dd9",
            "0x001788010db60916",
            "0x001788010db60976",
        ).mapIndexed { i, id ->
            HueBulb.Definition(
                id,
                "master_bedroom_light_${i}",
                Definition.Location.MASTER_BEDROOM
            )
        }

        val masterBathroomLights = listOf(
            "0x001788010db6035c",
            "0x001788010d87d7b9"
        ).mapIndexed { i, id -> HueBulb.Definition(id, "master_bathroom_light_${i}", MASTER_BATHROOM) }

        val sunroomLights = listOf(
            "0x001788010db60e3a",
            "0x001788010db61d0a",
            "0x001788010db5e845"
        ).mapIndexed { i, id -> HueBulb.Definition(id, "sunroom_light_${i}", Definition.Location.SUNROOM) }

        val smallBedroomLights = listOf(
            "0x001788010d87d95f",
            "0x001788010db5e8eb"
        ).mapIndexed { i, id -> HueBulb.Definition(id, "small_bedroom_light_${i}", Definition.Location.SUNROOM) }

        val officeLights = listOf(
            "0x001788010db6141e",
            "0x001788010db5e593",
            "0x001788010db60388",
            "0x001788010db5e5b8"
        ).mapIndexed { i, id -> HueBulb.Definition(id, "office_light_${i}", Definition.Location.SUNROOM) }


        val lights =
            kitchenLights + masterBedroomLights + sunroomLights + masterBathroomLights + walkinClosetLights +
                    livingRoomLights + diningRoomLights + smallBedroomLights + officeLights

        val hueDimmerIds = listOf("0x001788010b78fa91")

        val hueDimmers = hueDimmerIds
            .mapIndexed { i, id -> HueDimmer.Definition(id, "hue_dimmer_${i}", KITCHEN) }

        val sunroomSwitch = InovelliSwitch.Definition(
            "0x943469fffe05d0ab", "sunroom_switch",
            Definition.Location.SUNROOM
        )
        val masterBedroomSwitch = InovelliSwitch.Definition(
            "0x943469fffe05cce6", "master_bedroom_switch",
            Definition.Location.MASTER_BEDROOM
        )

        val smallBedroomSwitch = InovelliSwitch.Definition(
            "0x385b44fffeee1841", "small_bedroom_switch",
            Definition.Location.SMALL_BEDROOM
        )

        val officeSwitch = InovelliSwitch.Definition(
            "0x943469fffe05ca06", "office_switch",
            Definition.Location.OFFICE
        )

        val walkInClosetSwitch =
            InovelliSwitch.Definition("0x943469fffe05d0ae", "walk_in_closet_switch", MASTER_BATHROOM)

        val temperatureSensors = listOf(
            ZigbeeDevice.GenericDefinition("0x00158d000a9747e0", "freezer_sensor", KITCHEN),
            ZigbeeDevice.GenericDefinition("0x00158d000968662f", "small_bedroom_temp", Definition.Location.SMALL_BEDROOM),
        )

        val smallBedroomCurtains =
            listOf("0x0ceff6fffeaa1c2e", "0x4c97a1fffe279d92").mapIndexed {
                i, id -> ZigbeeDevice.GenericDefinition(
                    id = id,
                    name = "small_bedroom_curtain_$i",
                    location = Definition.Location.SMALL_BEDROOM,
            )
            }

        val pantryCurtain = listOf(ZigbeeDevice.GenericDefinition(
            id = "0x7c31fafffe3df31a",
            name = "pantry_curtain",
            location = Definition.Location.PANTRY,
        ))


        val livingRoomCurtains =
            listOf("0xf0fd45fffef89df7", "0xf0fd45fffef80f02", "0xf0fd45fffef82aba").mapIndexed {
                    i, id -> ZigbeeDevice.GenericDefinition(
                id = id,
                name = "living_room_curtain_$i",
                location = Definition.Location.SMALL_BEDROOM,
            )
            }

        val masterBedroomCurtains =
            listOf("0x7c31fafffe3d694a", "0x7c31fafffe3dd3f8").mapIndexed {
                i, id -> ZigbeeDevice.GenericDefinition(
                    id = id,
                    name = "master_bedroom_curtain_$i",
                    location = Definition.Location.MASTER_BEDROOM,
                )
            }

        val diningRoomCurtains =
            listOf("0x7c31fafffe3d678b", "0xf0fd45fffef71721", "0xf0fd45fffef58e5b").mapIndexed {
                    i, id -> ZigbeeDevice.GenericDefinition(
                id = id,
                name = "dining_room_curtain_$i",
                location = Definition.Location.DINING_ROOM,
            )
            }






        val definitions = lights +
                hueDimmers +
                listOf(sunroomSwitch, masterBedroomSwitch, walkInClosetSwitch, smallBedroomSwitch, officeSwitch) +
                temperatureSensors +
                smallBedroomCurtains +
                pantryCurtain +
                livingRoomCurtains +
                masterBedroomCurtains +
                diningRoomCurtains
    }

    val devices = Definitions.definitions.map { definition ->
        when (definition) {
            is HueBulb.Definition -> HueBulb(definition, mqttClient)
            is HueDimmer.Definition -> HueDimmer(definition, mqttClient)
            is InovelliSwitch.Definition -> InovelliSwitch(definition, mqttClient)
            is ZigbeeDevice.GenericDefinition -> ZigbeeDevice(definition, mqttClient)
            else -> throw IllegalArgumentException("Unknown device definition: $definition")
        }
    }

    val byName = devices.associateBy { it.definition.name }
    val byId = devices.associateBy { it.definition.id }

    val kitchenLights = Definitions.kitchenLights.map {
        byId[it.id] as HueBulb
    }
    val livingRoomLights = Definitions.livingRoomLights.map {
        byId[it.id] as HueBulb
    }
    val diningRoomLights = Definitions.diningRoomLights.map {
        byId[it.id] as HueBulb
    }
    val hueDimmers = Definitions.hueDimmers.map {
        byId[it.id] as HueDimmer
    }

    val masterBedroomLights = Definitions.masterBedroomLights.map {
        byId[it.id] as HueBulb
    }

    val sunroomLights = Definitions.sunroomLights.map {
        byId[it.id] as HueBulb
    }
    val masterBathroomLights = Definitions.masterBathroomLights.map {
        byId[it.id] as HueBulb
    }
    val walkInClosetLights = Definitions.walkinClosetLights.map {
        byId[it.id] as HueBulb
    }

    val smallBedroomLights = Definitions.smallBedroomLights.map {
        byId[it.id] as HueBulb
    }

    val officeLights = Definitions.officeLights.map {
        byId[it.id] as HueBulb
    }

    val masterBedroomDimmer = byId[Definitions.masterBedroomSwitch.id] as InovelliSwitch
    val sunroomDimmer = byId[Definitions.sunroomSwitch.id] as InovelliSwitch
    val walkInClosetDimmer = byId[Definitions.walkInClosetSwitch.id] as InovelliSwitch
    val smallBedroomDimmer = byId[Definitions.smallBedroomSwitch.id] as InovelliSwitch
    val officeDimmer = byId[Definitions.officeSwitch.id] as InovelliSwitch



    //val lights = kitchenLights + masterBedroomLights + sunroomLights + masterBathroomLights + walkInClosetLights

    fun watchForRestart(scope: CoroutineScope) {
        scope.launch {
            mqttClient.subscribe("zigbee2mqtt/bridge/state", scope)
                .filter { it?.contains("online") == true }
                .collect {
                    logger.info { "Zigbee2MQTT came online, re-initializing devices" }
                    devices.forEach { device ->
                        try {
                            device.reInitialize()
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to re-initialize device ${device.definition.name}" }
                        }
                    }
                }
        }
    }
}