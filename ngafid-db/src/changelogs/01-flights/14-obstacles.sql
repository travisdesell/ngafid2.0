--liquibase formatted sql

--changeset mingfeng:obstacles labels:flights,obstacles
CREATE TABLE obstacle_types (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(100) NOT NULL,

    PRIMARY KEY(id),
    UNIQUE KEY(name)
);

CREATE TABLE obstacles (
    id INT NOT NULL AUTO_INCREMENT,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    agl_height INT NOT NULL,
    msl_height INT NOT NULL,
    type_id INT NOT NULL,
    lighting_code VARCHAR(1) NOT NULL,
    quantity INT NOT NULL,

    PRIMARY KEY(id),
    FOREIGN KEY(type_id) REFERENCES obstacle_types(id)
        ON DELETE CASCADE
);

--changeset mingfeng:obstacles-type-data labels:flights,obstacles
INSERT INTO obstacle_types (name, description) 
    VALUES ("AG EQUIP","Agricultural Equipment"),
        ("AMUSEMENT PARK","Amusement Park Structure"),
        ("ANTENNA","Antenna"),
        ("ARCH","Arch"),
        ("BALLOON","Tethered Balloon (weather; other reconnaissance)"),
        ("BLDG","Building"),
        ("BLDG-TWR","Building Tower (latticework greater than 20' on building)"),
        ("BRIDGE","Bridge"),
        ("CABLE CAR","Cable Car"),
        ("CATENARY","Catenary (transmission line span/wire/cable)"),
        ("COOL TWR","Cooling Tower (nuclear cooling tower)"),
        ("CRANE","Crane (permanent)"),
        ("CTRL TWR","Control Tower (airport control tower)"),
        ("DAM","Dam"),
        ("DOME","Dome"),
        ("ELEC SYS","Electrical System (components of an electrical distribution system)"),
        ("ELEVATOR","Elevator"),
        ("FENCE","Fence"),
        ("GATE","Gate"),
        ("GEN UTIL","General Utility (components of a general utility system)"),
        ("GRAIN ELEVATOR","Grain Elevator"),
        ("HANGAR","Hangar"),
        ("HEAT COOL SYSTEM","Heat Cool System (components of a heating and cooling distribution system)"),
        ("LANDFILL","Landfill"),
        ("LGTHOUSE","Lighthouse"),
        ("MET","Meteorological Tower"),
        ("MONUMENT","Monument"),
        ("NATURAL GAS SYSTEM","Natural Gas System (components of a natural gas distribution system)"),
        ("NAVAID","Airport Navigational Aid"),
        ("PIPELINE PIPE","Pipeline Pipe"),
        ("PLANT","Plant"),
        ("POLE","Pole (flag pole; light pole)"),
        ("POWER PLANT","Power Plant (buildings and equipment used for creating electric/nuclear power)"),
        ("REFINERY","Refinery (buildings and equipment used for purifying crude materials)"),
        ("RIG","Rig (off-shore platform)"),
        ("SHIP","Ship"),
        ("SIGN","Sign"),
        ("SILO","Silo"),
        ("SOLAR PANELS","Solar Panels"),
        ("SPIRE","Spire (steeple)"),
        ("STACK","Stack (chimney; industrial smokestack)"),
        ("STADIUM","Stadium"),
        ("T-L TWR","Transmission Line Tower"),
        ("TANK","Tank (water; fuel)"),
        ("TETRAHEDRON","Tetrahedron (solid, pyramid-shaped landing direction indicator)"),
        ("TOWER","Tower"),
        ("TRAMWAY","Tramway"),
        ("TRAMWAY PYLON","Tramway Pylon"),
        ("UTILITY POLE","Utility Pole (telephone pole, or pole of similar height, supporting wires)"),
        ("VERTICAL STRUCTURE","Vertical Structure"),
        ("WALL","Wall"),
        ("WIND INDICATOR","Wind Indicator"),
        ("WINDMILL","Windmill (wind turbine)"),
        ("WINDSOCK","Windsock (flexible, mast-mounted cylinder showing wind direction and strength)");