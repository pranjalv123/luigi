package io.pranjal.home.devices

import io.pranjal.home.devices.ZigbeeDevice.Definition.Location.KITCHEN
import io.pranjal.home.devices.ZigbeeDevice.Definition.Location.MASTER_BATHROOM
import io.pranjal.home.devices.makes.HueBulb
import io.pranjal.home.devices.makes.HueDimmer
import io.pranjal.home.devices.makes.InovelliSwitch
import io.pranjal.mqtt.MqttClient

class Devices(mqttClient: MqttClient) {
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
                ZigbeeDevice.Definition.Location.DINING_ROOM
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
                ZigbeeDevice.Definition.Location.LIVING_ROOM
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
                ZigbeeDevice.Definition.Location.MASTER_BEDROOM
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
        ).mapIndexed { i, id -> HueBulb.Definition(id, "sunroom_light_${i}", ZigbeeDevice.Definition.Location.SUNROOM) }

        val smallBedroomLights = listOf(
            "0x001788010d87d95f",
            "0x001788010db5e8eb"
        ).mapIndexed { i, id -> HueBulb.Definition(id, "small_bedroom_light_${i}", ZigbeeDevice.Definition.Location.SUNROOM) }

        val officeLights = listOf(
            "0x001788010db6141e",
            "0x001788010db5e593",
            "0x001788010db60388",
            "0x001788010db5e5b8\n"
        ).mapIndexed { i, id -> HueBulb.Definition(id, "office_light_${i}", ZigbeeDevice.Definition.Location.SUNROOM) }


        val lights =
            kitchenLights + masterBedroomLights + sunroomLights + masterBathroomLights + walkinClosetLights +
                    livingRoomLights + diningRoomLights + smallBedroomLights

        val hueDimmerIds = listOf("0x001788010b78fa91")

        val hueDimmers = hueDimmerIds
            .mapIndexed { i, id -> HueDimmer.Definition(id, "hue_dimmer_${i}", KITCHEN) }

        val sunroomSwitch = InovelliSwitch.Definition(
            "0x943469fffe05d0ab", "sunroom_switch",
            ZigbeeDevice.Definition.Location.SUNROOM
        )
        val masterBedroomSwitch = InovelliSwitch.Definition(
            "0x943469fffe05cce6", "master_bedroom_switch",
            ZigbeeDevice.Definition.Location.MASTER_BEDROOM
        )

        val smallBedroomSwitch = InovelliSwitch.Definition(
            "0x385b44fffeee1841", "small_bedroom_switch",
            ZigbeeDevice.Definition.Location.SMALL_BEDROOM
        )

        val officeSwitch = InovelliSwitch.Definition(
            "0x943469fffe05ca06", "office_switch",
            ZigbeeDevice.Definition.Location.OFFICE
        )

        val walkInClosetSwitch =
            InovelliSwitch.Definition("0x943469fffe05d0ae", "walk_in_closet_switch", MASTER_BATHROOM)

        val definitions = lights + hueDimmers + listOf(sunroomSwitch, masterBedroomSwitch, walkInClosetSwitch, smallBedroomSwitch, officeSwitch)
    }

    val devices = Definitions.definitions.map { definition ->
        when (definition) {
            is HueBulb.Definition -> HueBulb(definition, mqttClient)
            is HueDimmer.Definition -> HueDimmer(definition, mqttClient)
            is InovelliSwitch.Definition -> InovelliSwitch(definition, mqttClient)
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

}