package org.ngafid.core.obstacles;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.kafka.common.protocol.types.Field.Bool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
// import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.ngafid.core.obstacles.MarkedObstacle.ObstacleRisk;
import org.ngafid.core.obstacles.Obstacle.Lighting;

public class ObstaclesTest {

    static final String DAUPHIN_ISLAND = """
        -88.109366667,30.255975,21,01-047953,U,US,AL,DAUPHIN ISLAND,30 15 21.51N,088 06 33.72W,30.255975,-88.109367,TOWER,1,118,124,N,4,D,N,2018ASO21066OE,C,2020125
        -88.116422222,30.256716667,22,01-049522,O,US,AL,DAUPHIN ISLAND,30 15 24.18N,088 06 59.12W,30.256717,-88.116422,ELEC SYS,1,20,24,N,4,D,N,2017ASO18976OE,A,2018053
        -88.1150638889999,30.256783333,23,01-057729,U,US,AL,DAUPHIN ISLAND,30 15 24.42N,088 06 54.23W,30.256783,-88.115064,TOWER,1,119,123,N,4,D,N,2018ASO15175OE,A,2019205
        -88.1161611109999,30.2568694440001,24,01-065448,U,US,AL,DAUPHIN ISLAND,30 15 24.73N,088 06 58.18W,30.256869,-88.116161,T-L TWR,1,61,65,N,4,D,N,2020ASO25194OE,A,2022220
        -88.1141666669999,30.2568694440001,25,01-057836,U,US,AL,DAUPHIN ISLAND,30 15 24.73N,088 06 51.00W,30.256869,-88.114167,TOWER,1,111,117,D,4,D,N,2019ASO25695OE,A,2019311
        -88.115875,30.256875,26,01-065447,U,US,AL,DAUPHIN ISLAND,30 15 24.75N,088 06 57.15W,30.256875,-88.115875,T-L TWR,1,61,65,N,4,D,N,2020ASO25193OE,A,2022220
        -88.1164277779999,30.256875,27,01-049523,O,US,AL,DAUPHIN ISLAND,30 15 24.75N,088 06 59.14W,30.256875,-88.116428,ELEC SYS,1,20,24,N,4,D,N,2017ASO18977OE,A,2018053
        -88.116336111,30.2568805560001,28,01-065449,U,US,AL,DAUPHIN ISLAND,30 15 24.77N,088 06 58.81W,30.256881,-88.116336,T-L TWR,1,57,61,N,4,D,N,2020ASO25195OE,A,2022220
        -88.1149,30.256886111,29,01-049162,U,US,AL,DAUPHIN ISLAND,30 15 24.79N,088 06 53.64W,30.256886,-88.1149,T-L TWR,1,66,70,N,4,D,S,2017ASO16652OE,C,2017352
        -88.114883333,30.256886111,30,01-065445,U,US,AL,DAUPHIN ISLAND,30 15 24.79N,088 06 53.58W,30.256886,-88.114883,T-L TWR,1,66,69,N,4,D,N,2020ASO25191OE,A,2022220
        -88.115497222,30.256888889,31,01-065446,U,US,AL,DAUPHIN ISLAND,30 15 24.80N,088 06 55.79W,30.256889,-88.115497,T-L TWR,1,61,66,N,4,D,N,2020ASO25192OE,A,2022220
        -87.632777778,30.2569444440001,32,01-065330,U,US,AL,ORANGE BEACH,30 15 25.00N,087 37 58.00W,30.256944,-87.632778,BLDG,1,315,333,R,4,D,N,2020ASO07366OE,A,2022235
        -88.112627778,30.2573472220001,33,01-065440,U,US,AL,DAUPHIN ISLAND,30 15 26.45N,088 06 45.46W,30.257347,-88.112628,T-L TWR,1,75,80,N,4,D,N,2020ASO25186OE,A,2022220
        -88.112633333,30.257352778,34,01-049151,U,US,AL,DAUPHIN ISLAND,30 15 26.47N,088 06 45.48W,30.257353,-88.112633,T-L TWR,1,75,80,N,4,D,S,2017ASO16644OE,C,2018011
        -88.1143777779999,30.2573583330001,35,01-065443,U,US,AL,DAUPHIN ISLAND,30 15 26.49N,088 06 51.76W,30.257358,-88.114378,T-L TWR,1,61,67,N,4,D,N,2020ASO25189OE,A,2022220
        -88.113313889,30.257361111,36,01-049158,U,US,AL,DAUPHIN ISLAND,30 15 26.50N,088 06 47.93W,30.257361,-88.113314,T-L TWR,1,70,76,N,4,D,S,2017ASO16646OE,C,2017352
        -88.1132805559999,30.257361111,37,01-065441,U,US,AL,DAUPHIN ISLAND,30 15 26.50N,088 06 47.81W,30.257361,-88.113281,T-L TWR,1,70,75,N,4,D,N,2020ASO25187OE,A,2022220
        -88.114888889,30.257361111,38,01-065444,U,US,AL,DAUPHIN ISLAND,30 15 26.50N,088 06 53.60W,30.257361,-88.114889,T-L TWR,1,66,71,N,4,D,N,2020ASO25190OE,A,2022220
        -88.114902778,30.257363889,39,01-049159,U,US,AL,DAUPHIN ISLAND,30 15 26.51N,088 06 53.65W,30.257364,-88.114903,T-L TWR,1,66,71,N,4,D,N,2017ASO16650OE,C,2018057
        -88.113922222,30.257363889,40,01-065442,U,US,AL,DAUPHIN ISLAND,30 15 26.51N,088 06 50.12W,30.257364,-88.113922,T-L TWR,1,66,71,N,4,D,N,2020ASO25188OE,A,2022220
        -88.1139277779999,30.257366667,41,01-049155,U,US,AL,DAUPHIN ISLAND,30 15 26.52N,088 06 50.14W,30.257367,-88.113928,T-L TWR,1,66,71,N,4,D,N,2017ASO16648OE,C,2018060
        -88.1126833329999,30.258144444,42,01-049153,U,US,AL,DAUPHIN ISLAND,30 15 29.32N,088 06 45.66W,30.258144,-88.112683,T-L TWR,1,70,74,N,4,D,N,2017ASO16642OE,C,2018026
        -88.106994444,30.2583527780001,43,01-074753,U,US,AL,DAUPHIN ISLAND,30 15 30.07N,088 06 25.18W,30.258353,-88.106994,TOWER,1,120,125,R,4,D,N,2024ASO07117OE,A,2024221
        -87.709338889,30.2588638890001,44,01-065295,U,US,AL,GULF SHORES,30 15 31.91N,087 42 33.62W,30.258864,-87.709339,TOWER,1,190,201,N,4,D,N,2021ASO40881OE,A,2022195
        -88.112822222,30.258927778,45,01-049149,U,US,AL,DAUPHIN ISLAND,30 15 32.14N,088 06 46.16W,30.258928,-88.112822,T-L TWR,1,66,71,N,4,D,N,2017ASO16640OE,C,2019084
    """;

    static final String ORANGE_BEACH = """
        87.584444444,30.282777778,94,01-002870,O,US,AL,ORANGE BEACH,30 16 58.00N,087 35 04.00W,30.282778,-87.584444,TOWER,1,155,170,N,2,C,N,2003ASO00528OE,C,2003341
        -87.7291749999999,30.284019444,95,01-074927,U,US,AL,GULF SHORES,30 17 02.47N,087 43 45.03W,30.284019,-87.729175,TOWER,1,200,203,N,1,A,N,2022ASO27169OE,A,2024352
        -87.615391667,30.2869277780001,96,01-002734,O,US,AL,ORANGE BEACH,30 17 12.94N,087 36 55.41W,30.286928,-87.615392,TOWER,1,159,169,D,2,C,N,2001ASO09021OE,C,2004291
        -87.682016667,30.287594444,97,01-021529,O,US,AL,GULF SHORES,30 17 15.34N,087 40 55.26W,30.287594,-87.682017,BLDG,1,34,47,U,2,C,U,,A,2012334
        -87.616422222,30.2876666670001,98,01-074902,U,US,AL,ORANGE BEACH,30 17 15.60N,087 36 59.12W,30.287667,-87.616422,POLE,1,80,92,N,4,D,N,2023ASO32298OE,A,2024330
        -87.615211111,30.287738889,99,01-074900,U,US,AL,ORANGE BEACH,30 17 15.86N,087 36 54.76W,30.287739,-87.615211,POLE,1,80,92,N,4,D,N,2023ASO32267OE,A,2024330
        -87.6797861109999,30.28815,100,01-021528,O,US,AL,GULF SHORES,30 17 17.34N,087 40 47.23W,30.28815,-87.679786,TOWER,1,61,74,U,2,C,U,,A,2012334
        -87.633808333,30.2882805560001,101,01-002761,O,US,AL,ORANGE BEACH,30 17 17.81N,087 38 01.71W,30.288281,-87.633808,TOWER,1,151,160,N,1,C,N,2013ASO04964OE,C,2017131
        -87.615211111,30.288333333,102,01-074901,U,US,AL,ORANGE BEACH,30 17 18.00N,087 36 54.76W,30.288333,-87.615211,POLE,1,80,92,N,4,D,N,2023ASO32268OE,A,2024330
        -87.616422222,30.288402778,103,01-074903,U,US,AL,ORANGE BEACH,30 17 18.25N,087 36 59.12W,30.288403,-87.616422,POLE,1,80,92,N,4,D,N,2023ASO32299OE,A,2024330
        -87.638313889,30.289022222,104,01-002672,O,US,AL,ORANGE BEACH,30 17 20.48N,087 38 17.93W,30.289022,-87.638314,TOWER,1,163,170,D,1,A,N,2022ASO19030OE,C,2023136
        -88.1283333329999,30.2891666670001,105,01-000180,O,US,AL,DAUPHIN ISLAND,30 17 21.00N,088 07 42.00W,30.289167,-88.128333,BRIDGE,2,150,150,U,9,I,U,,C,2014138
        -87.684691667,30.289897222,106,01-021524,O,US,AL,GULF SHORES,30 17 23.63N,087 41 04.89W,30.289897,-87.684692,POLE,1,38,51,U,2,C,U,,A,2012334
        -87.614913889,30.2901666670001,107,01-063394,U,US,AL,ORANGE BEACH,30 17 24.60N,087 36 53.69W,30.290167,-87.614914,POLE,1,37,47,N,4,D,N,2018ASO09332OE,A,2021228
        -87.602625,30.2902111110001,108,01-063397,U,US,AL,ORANGE BEACH,30 17 24.76N,087 36 09.45W,30.290211,-87.602625,POLE,1,37,56,N,4,D,N,2018ASO09335OE,A,2021228
        -87.602411111,30.2902111110001,109,01-063398,U,US,AL,ORANGE BEACH,30 17 24.76N,087 36 08.68W,30.290211,-87.602411,POLE,1,37,56,N,4,D,N,2018ASO09336OE,A,2021229
        -87.684519444,30.2903166670001,110,01-021525,O,US,AL,GULF SHORES,30 17 25.14N,087 41 04.27W,30.290317,-87.684519,POLE,1,36,50,U,2,C,U,,A,2012334
        -87.6152361109999,30.290386111,111,01-063393,U,US,AL,ORANGE BEACH,30 17 25.39N,087 36 54.85W,30.290386,-87.615236,POLE,1,37,47,N,4,D,N,2018ASO09331OE,A,2021235
        -87.614894444,30.2903944440001,112,01-063395,U,US,AL,ORANGE BEACH,30 17 25.42N,087 36 53.62W,30.290394,-87.614894,POLE,1,37,46,N,4,D,N,2018ASO09333OE,A,2021228
        -87.602622222,30.290430556,113,01-063396,U,US,AL,ORANGE BEACH,30 17 25.55N,087 36 09.44W,30.290431,-87.602622,POLE,1,37,55,N,4,D,N,2018ASO09334OE,A,2021228
        -87.637175,30.2904444440001,114,01-021520,O,US,AL,GULF SHORES,30 17 25.60N,087 38 13.83W,30.290444,-87.637175,T-L TWR,1,138,146,U,2,C,U,,A,2012334
        -87.684344444,30.2907527780001,115,01-021526,O,US,AL,GULF SHORES,30 17 26.71N,087 41 03.64W,30.290753,-87.684344,POLE,1,34,49,U,2,C,U,,A,2012334
        -87.6790583329999,30.2909472220001,116,01-021523,O,US,AL,GULF SHORES,30 17 27.41N,087 40 44.61W,30.290947,-87.679058,SIGN,1,5,18,U,2,C,U,,A,2012334
        -87.684936111,30.2911861110001,117,01-047811,U,US,AL,GULFSHORES,30 17 28.27N,087 41 05.77W,30.291186,-87.684936,BLDG,1,31,45,N,4,D,N,2014ASO00405OE,A,2018024
        -87.684186111,30.291194444,118,01-021527,O,US,AL,GULF SHORES,30 17 28.30N,087 41 03.07W,30.291194,-87.684186,POLE,1,33,49,U,2,C,U,,A,2012334
        -87.581741667,30.2913833330001,119,01-063401,U,US,AL,ORANGE BEACH,30 17 28.98N,087 34 54.27W,30.291383,-87.581742,POLE,1,37,54,N,4,D,N,2018ASO09339OE,A,2021229
        -87.6610416669999,30.291419444,120,01-021522,O,US,AL,GULF SHORES,30 17 29.11N,087 39 39.75W,30.291419,-87.661042,TOWER,1,46,60,R,2,C,U,,A,2012334
        -87.581902778,30.291483333,121,01-063400,U,US,AL,ORANGE BEACH,30 17 29.34N,087 34 54.85W,30.291483,-87.581903,POLE,1,37,54,N,4,D,N,2018ASO09338OE,A,2021228
        -87.582733333,30.29155,122,01-063402,U,US,AL,ORANGE BEACH,30 17 29.58N,087 34 57.84W,30.29155,-87.582733,POLE,1,28,45,N,4,D,N,2018ASO09340OE,A,2021228
        -87.581677778,30.291747222,123,01-063399,U,US,AL,ORANGE BEACH,30 17 30.29N,087 34 54.04W,30.291747,-87.581678,POLE,1,37,58,N,4,D,N,2018ASO09337OE,A,2021228
        -87.581705556,30.291855556,124,01-063403,U,US,AL,ORANGE BEACH,30 17 30.68N,087 34 54.14W,30.291856,-87.581706,POLE,1,28,49,N,4,D,N,2018ASO09341OE,A,2021228
    """;

    static final String GULF_SHORE = """
        -87.6371694439999,30.291958333,125,01-021521,O,US,AL,GULF SHORES,30 17 31.05N,087 38 13.81W,30.291958,-87.637169,T-L TWR,1,140,148,U,2,C,U,,A,2012334
        -87.6831444439999,30.29355,126,01-063308,U,US,AL,GULF SHORES,30 17 36.78N,087 40 59.32W,30.29355,-87.683144,POLE,1,37,55,N,4,D,N,2020ASO12113OE,A,2021175
        -87.6806194439999,30.293561111,127,01-067147,U,US,AL,GULF SHORES,30 17 36.82N,087 40 50.23W,30.293561,-87.680619,SIGN,1,12,27,N,4,D,N,2021ASO01654OE,A,2022326
        -87.683141667,30.293572222,128,01-063312,U,US,AL,GULF SHORES,30 17 36.86N,087 40 59.31W,30.293572,-87.683142,POLE,1,8,29,N,4,D,N,2020ASO12117OE,A,2021176
        -87.683680556,30.293683333,129,01-063309,U,US,AL,GULF SHORES,30 17 37.26N,087 41 01.25W,30.293683,-87.683681,POLE,1,37,52,N,4,D,N,2020ASO12114OE,A,2021176
        -87.683638889,30.2936861110001,130,01-063313,U,US,AL,GULF SHORES,30 17 37.27N,087 41 01.10W,30.293686,-87.683639,POLE,1,8,24,N,4,D,N,2020ASO12118OE,A,2021176
        -87.683113889,30.2937916670001,131,01-063311,U,US,AL,GULF SHORES,30 17 37.65N,087 40 59.21W,30.293792,-87.683114,POLE,1,8,29,N,4,D,N,2020ASO12116OE,A,2021176
        -87.6831444439999,30.293825,132,01-063307,U,US,AL,GULF SHORES,30 17 37.77N,087 40 59.32W,30.293825,-87.683144,POLE,1,37,54,N,4,D,N,2020ASO12112OE,A,2021175
        -87.6805555559999,30.2938361110001,133,01-067148,U,US,AL,GULF SHORES,30 17 37.81N,087 40 50.00W,30.293836,-87.680556,SIGN,1,11,26,N,4,D,N,2021ASO01655OE,A,2022328
        -87.6835944439999,30.2938833330001,134,01-063314,U,US,AL,GULF SHORES,30 17 37.98N,087 41 00.94W,30.293883,-87.683594,POLE,1,8,25,N,4,D,N,2020ASO12119OE,A,2021176
        -87.683641667,30.293891667,135,01-063310,U,US,AL,GULF SHORES,30 17 38.01N,087 41 01.11W,30.293892,-87.683642,POLE,1,37,53,N,4,D,N,2020ASO12115OE,A,2021176
        -87.680319444,30.294291667,136,01-067149,U,US,AL,GULF SHORES,30 17 39.45N,087 40 49.15W,30.294292,-87.680319,SIGN,1,13,31,N,4,D,N,2021ASO01656OE,A,2022326
        -87.679444444,30.2944166670001,137,01-049366,O,US,AL,GULF SHORES,30 17 39.90N,087 40 46.00W,30.294417,-87.679444,BLDG,1,50,69,N,4,D,N,,A,2019029
        -87.6806,30.294805556,138,01-067150,U,US,AL,GULF SHORES,30 17 41.30N,087 40 50.16W,30.294806,-87.6806,SIGN,1,10,29,N,4,D,N,2021ASO01657OE,A,2022325
    """;

    static final String FAIR_HOPE = """
        -87.8523,30.4127722220001,224,01-046585,U,US,AL,FAIRHOPE,30 24 45.98N,087 51 08.28W,30.412772,-87.8523,T-L TWR,1,70,115,N,4,D,N,2011ASO06699OE,A,2018027
        -87.852302778,30.414222222,225,01-046548,U,US,AL,FAIRHOPE,30 24 51.20N,087 51 08.29W,30.414222,-87.852303,T-L TWR,1,79,122,N,4,D,N,2011ASO06700OE,A,2018027
        -87.673230556,30.4155388890001,226,01-002755,O,US,AL,FOLEY,30 24 55.94N,087 40 23.63W,30.415539,-87.673231,TOWER,1,205,275,D,1,A,N,2023ASO24083OE,C,2024080
        -87.852305556,30.4157500000001,227,01-046570,U,US,AL,FAIRHOPE,30 24 56.70N,087 51 08.30W,30.41575,-87.852306,T-L TWR,1,70,112,N,4,D,N,2011ASO06701OE,A,2018027
        -87.8523083329999,30.4171638890001,228,01-046586,U,US,AL,FAIRHOPE,30 25 01.79N,087 51 08.31W,30.417164,-87.852308,T-L TWR,1,79,117,N,4,D,N,2011ASO06702OE,A,2018027
        -87.531666667,30.4177777780001,229,01-002540,O,US,AL,ELBERTA,30 25 04.00N,087 31 54.00W,30.417778,-87.531667,TOWER,1,349,422,D,2,C,N,2000ASO08699OE,C,2001351
        -87.8523111109999,30.418538889,230,01-046549,U,US,AL,FAIRHOPE,30 25 06.74N,087 51 08.32W,30.418539,-87.852311,T-L TWR,1,70,105,N,4,D,N,2011ASO06703OE,A,2018027
        -87.746055556,30.4191277780001,231,01-001318,O,US,AL,FOLEY,30 25 08.86N,087 44 45.80W,30.419128,-87.746056,TOWER,1,401,480,R,1,A,P,2017ASO01797OE,C,2017230
        -87.852313889,30.419869444,232,01-046571,U,US,AL,FAIRHOPE,30 25 11.53N,087 51 08.33W,30.419869,-87.852314,T-L TWR,1,93,127,N,4,D,N,2011ASO06704OE,A,2018027
        -87.853505556,30.4201833330001,233,01-046587,U,US,AL,FAIRHOPE,30 25 12.66N,087 51 12.62W,30.420183,-87.853506,T-L TWR,1,88,133,N,4,D,N,2011ASO06705OE,A,2018027
        -87.730686111,30.420452778,234,01-001665,O,US,AL,FOLEY,30 25 13.63N,087 43 50.47W,30.420453,-87.730686,TOWER,1,307,349,D,1,A,N,2010ASO03477OE,C,2010354
        -87.8546638889999,30.420494444,235,01-046550,U,US,AL,FAIRHOPE,30 25 13.78N,087 51 16.79W,30.420494,-87.854664,T-L TWR,1,88,140,N,4,D,N,2011ASO06706OE,A,2018027
        -87.855863889,30.420811111,236,01-046572,U,US,AL,FAIRHOPE,30 25 14.92N,087 51 21.11W,30.420811,-87.855864,T-L TWR,1,88,145,N,4,D,N,2011ASO06707OE,A,2018027
        -87.8582749999999,30.420986111,237,01-046551,U,US,AL,FAIRHOPE,30 25 15.55N,087 51 29.79W,30.420986,-87.858275,T-L TWR,1,84,151,N,4,D,N,2011ASO06709OE,A,2018027
        -87.859058333,30.4210833330001,238,01-046573,U,US,AL,FAIRHOPE,30 25 15.90N,087 51 32.61W,30.421083,-87.859058,T-L TWR,1,79,145,N,4,D,N,2011ASO06710OE,A,2018027
        -87.857027778,30.4210833330001,239,01-046588,U,US,AL,FAIRHOPE,30 25 15.90N,087 51 25.30W,30.421083,-87.857028,T-L TWR,1,93,155,N,4,D,N,2011ASO06708OE,A,2018027
        -87.859841667,30.4211500000001,240,01-046589,U,US,AL,FAIRHOPE,30 25 16.14N,087 51 35.43W,30.42115,-87.859842,T-L TWR,1,79,143,N,4,D,N,2011ASO06711OE,A,2018027
        -87.873308333,30.421169444,241,01-046577,U,US,AL,FAIRHOPE,30 25 16.21N,087 52 23.91W,30.421169,-87.873308,T-L TWR,1,88,136,N,4,D,N,2011ASO06722OE,A,2018027
    """;
    

    @AfterAll
    static void teardown() {
        HashMap<String, ArrayList<Obstacle>> geoHasMap = new HashMap<>();
        HashMap<Integer, Obstacle> obstacleMap = new HashMap<>();
        Obstacles.InjectTestData(geoHasMap, obstacleMap);
    }

    private void generateData(String csv) {
        HashMap<String, ArrayList<Obstacle>> geoHasMap = new HashMap<>();
        HashMap<Integer, Obstacle> obstacleMap = new HashMap<>();

        for (String line : csv.split("\\n")) {
            String[] values = line.split(",");
            int id = Integer.parseInt(values[2]);
            double lat = Double.parseDouble(values[10]);
            double lon = Double.parseDouble(values[11]);
            int agl = Integer.parseInt(values[14]);
            int amsl = Integer.parseInt(values[15]);
            String type = values[12];
            int quantity = Integer.parseInt(values[13]);
            Lighting lighting = Lighting.valueOf(values[16]);

            Obstacle obstacle = new Obstacle(id, lat, lon, type, agl, amsl, quantity, lighting);

            ArrayList<Obstacle> hashedObstacles = geoHasMap.computeIfAbsent(obstacle.getGeoHash(), k -> new ArrayList<>());
            hashedObstacles.add(obstacle);
            obstacleMap.put(obstacle.getID(), obstacle);
        }
        Obstacles.InjectTestData(geoHasMap, obstacleMap);
    }

    private void printObstacle(MarkedObstacle marked) {
        System.out.println(
            "Obstacle " + marked.getObstacleID() +
            " | hash=" + marked.getObstacle().getGeoHash() +
            " | horizontal=" + marked.getHorizontalDistance() +
            " | vertical=" + marked.getVerticalDistance() +
            " | distance=" + marked.getTotalDistance()
        );
    }

    @Test
    public void testParseObstacleNearDauphinIsland500() {

        generateData(DAUPHIN_ISLAND);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(25, 123.7);
        expectedIdsAndDistance.put(35, 124.5);
        expectedIdsAndDistance.put(41, 193.1);
        expectedIdsAndDistance.put(30, 193.7);
        expectedIdsAndDistance.put(40, 194.1);
        expectedIdsAndDistance.put(38, 196.3);
        expectedIdsAndDistance.put(29, 197.9);
        expectedIdsAndDistance.put(39, 200.3);
        expectedIdsAndDistance.put(23, 244.7);
        expectedIdsAndDistance.put(36, 362.3);
        expectedIdsAndDistance.put(31, 366.8);
        expectedIdsAndDistance.put(37, 372.1);
        expectedIdsAndDistance.put(26, 481.6);

        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange(30.257120, -88.114400, 150, 500);
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());

        for (MarkedObstacle marked : obstacles) {
            // Rounding to int to remove floating point imprecision
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testParseObstacleNearDauphinIsland1000() {
        generateData(DAUPHIN_ISLAND);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(25, 123.7);
        expectedIdsAndDistance.put(35, 124.5);
        expectedIdsAndDistance.put(41, 193.1);
        expectedIdsAndDistance.put(30, 193.7);
        expectedIdsAndDistance.put(40, 194.1);
        expectedIdsAndDistance.put(38, 196.3);
        expectedIdsAndDistance.put(29, 197.9);
        expectedIdsAndDistance.put(39, 200.3);
        expectedIdsAndDistance.put(23, 244.7);
        expectedIdsAndDistance.put(36, 362.3);
        expectedIdsAndDistance.put(31, 366.8);
        expectedIdsAndDistance.put(37, 372.1);
        expectedIdsAndDistance.put(26, 481.6);
        expectedIdsAndDistance.put(34, 568.2);
        expectedIdsAndDistance.put(24, 569.4);
        expectedIdsAndDistance.put(33, 569.3);
        expectedIdsAndDistance.put(28, 623.2);
        expectedIdsAndDistance.put(27, 658.2);
        expectedIdsAndDistance.put(42, 662.3);
        expectedIdsAndDistance.put(22, 666.7);
        expectedIdsAndDistance.put(45, 830.3);

        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange(30.257120, -88.114400, 150, 1000);
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());
        
        for (MarkedObstacle marked : obstacles) {
            // Rounding to int to remove floating point imprecision
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testParseObstacleNearDauphinIsland1500() {
        generateData(DAUPHIN_ISLAND);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(25, 123.7);
        expectedIdsAndDistance.put(35, 124.5);
        expectedIdsAndDistance.put(41, 193.1);
        expectedIdsAndDistance.put(30, 193.7);
        expectedIdsAndDistance.put(40, 194.1);
        expectedIdsAndDistance.put(38, 196.3);
        expectedIdsAndDistance.put(29, 197.9);
        expectedIdsAndDistance.put(39, 200.3);
        expectedIdsAndDistance.put(23, 244.7);
        expectedIdsAndDistance.put(36, 362.3);
        expectedIdsAndDistance.put(31, 366.8);
        expectedIdsAndDistance.put(37, 372.1);
        expectedIdsAndDistance.put(26, 481.6);
        expectedIdsAndDistance.put(34, 568.2);
        expectedIdsAndDistance.put(24, 569.4);
        expectedIdsAndDistance.put(33, 569.3);
        expectedIdsAndDistance.put(28, 623.2);
        expectedIdsAndDistance.put(27, 658.2);
        expectedIdsAndDistance.put(42, 662.3);
        expectedIdsAndDistance.put(22, 666.7);
        expectedIdsAndDistance.put(45, 830.3);

        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange(30.257120, -88.114400, 150, 1500);
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());
        
        for (MarkedObstacle marked : obstacles) {
            // Rounding to int to remove floating point imprecision
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testParseOrangeBeach500() {
        generateData(ORANGE_BEACH);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.290059, -87.682052, 100, 500);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());
    }

    @Test
    public void testParseOrangeBeach1000() {
        generateData(ORANGE_BEACH);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.290059, -87.682052, 100, 1000);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(115, 767.8);
        expectedIdsAndDistance.put(110, 785.4);
        expectedIdsAndDistance.put(118, 791.6);
        expectedIdsAndDistance.put(106, 836.2);
        expectedIdsAndDistance.put(97, 901.8);
        expectedIdsAndDistance.put(100, 997.6);
        expectedIdsAndDistance.put(117, 999.7);
        
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());

        for (MarkedObstacle marked : obstacles) {
            // Rounding to int to remove floating point imprecision
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testParseOrangeBeach1500() {

        generateData(ORANGE_BEACH);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.290059, -87.682052, 100, 1500);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(115, 767.8);
        expectedIdsAndDistance.put(110, 785.4);
        expectedIdsAndDistance.put(118, 791.6);
        expectedIdsAndDistance.put(106, 836.2);
        expectedIdsAndDistance.put(97, 901.8);
        expectedIdsAndDistance.put(100, 997.6);
        expectedIdsAndDistance.put(117, 999.7);
        expectedIdsAndDistance.put(116, 1001.6);
        
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());

        for (MarkedObstacle marked : obstacles) {
            // Rounding to int to remove floating point imprecision
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testParseGulfShore500() {
        generateData(GULF_SHORE);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.293888, -87.682203, 100, 500);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(131, 303.1);
        expectedIdsAndDistance.put(132, 303.8);
        expectedIdsAndDistance.put(128, 330.5);
        expectedIdsAndDistance.put(126, 327.2);
        expectedIdsAndDistance.put(134, 447.7);
        expectedIdsAndDistance.put(135, 458.3);
        expectedIdsAndDistance.put(130, 467.3);
        expectedIdsAndDistance.put(129, 475.5);
        
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());

        for (MarkedObstacle marked : obstacles) {
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testParseGulfShore1000() {
        generateData(GULF_SHORE);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.293888, -87.682203, 100, 1000);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(131, 303.1);
        expectedIdsAndDistance.put(132, 303.8);
        expectedIdsAndDistance.put(128, 330.5);
        expectedIdsAndDistance.put(126, 327.2);
        expectedIdsAndDistance.put(134, 447.7);
        expectedIdsAndDistance.put(135, 458.3);
        expectedIdsAndDistance.put(130, 467.3);
        expectedIdsAndDistance.put(129, 475.5);
        expectedIdsAndDistance.put(127, 520.8);
        expectedIdsAndDistance.put(133, 527.1);
        expectedIdsAndDistance.put(138, 612.9);
        expectedIdsAndDistance.put(136, 617.5);
        expectedIdsAndDistance.put(137, 892.2);
        
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());

        for (MarkedObstacle marked : obstacles) {
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testParseGulfShore1500() {
        generateData(GULF_SHORE);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.293888, -87.682203, 100, 1500);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(131, 303.1);
        expectedIdsAndDistance.put(132, 303.8);
        expectedIdsAndDistance.put(128, 330.5);
        expectedIdsAndDistance.put(126, 327.2);
        expectedIdsAndDistance.put(134, 447.7);
        expectedIdsAndDistance.put(135, 458.3);
        expectedIdsAndDistance.put(130, 467.3);
        expectedIdsAndDistance.put(129, 475.5);
        expectedIdsAndDistance.put(127, 520.8);
        expectedIdsAndDistance.put(133, 527.1);
        expectedIdsAndDistance.put(138, 612.9);
        expectedIdsAndDistance.put(136, 617.5);
        expectedIdsAndDistance.put(137, 892.2);
        
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());

        for (MarkedObstacle marked : obstacles) {
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testParseFairHope500() {

        generateData(FAIR_HOPE);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.416254, -87.857078, 180, 500);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());
    }

    @Test
    public void testParseFairHope1000() {

        generateData(FAIR_HOPE);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.416254, -87.857078, 180, 1000);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());
    }

    @Test
    public void testParseFairHope1500() {

        generateData(FAIR_HOPE);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.416254, -87.857078, 180, 1500);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());
    }

    @Test
    public void testParseFairHope2000() {
        generateData(FAIR_HOPE);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.416254, -87.857078, 180, 2000);
        HashMap<Integer, Double> expectedIdsAndDistance = new HashMap<>();
        expectedIdsAndDistance.put(227, 1517.4);
        expectedIdsAndDistance.put(228, 1539.8);
        expectedIdsAndDistance.put(225, 1678.2);
        expectedIdsAndDistance.put(236, 1707.6);
        expectedIdsAndDistance.put(230, 1718.7);
        expectedIdsAndDistance.put(235, 1726.0);
        expectedIdsAndDistance.put(239, 1763.6);
        expectedIdsAndDistance.put(237, 1768.8);
        expectedIdsAndDistance.put(233, 1824.2);
        expectedIdsAndDistance.put(238, 1870.5);
        expectedIdsAndDistance.put(224, 1971.1);
        expectedIdsAndDistance.put(240, 1988.5);
        expectedIdsAndDistance.put(232, 1998.2);
        
        assertEquals(expectedIdsAndDistance.size(), obstacles.size());

        for (MarkedObstacle marked : obstacles) {
            int expected = (int) Math.round(expectedIdsAndDistance.get(marked.getObstacleID()));
            int result = (int) Math.round(marked.getTotalDistance());
            assertEquals(expected, result);
        }
    }

    @Test
    public void testHighRiskObstacle() {
        String csv = "-87.681418,30.290059,999,01-099999,U,US,AL,GULF SHORES,30 17 24.21N,087 40 53.10W,30.290059,-87.681418,TOWER,1,110,130,N,4,D,N,2026ASO00001OE,A,2026223";
        generateData(csv);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.290059, -87.682052, 100, 1000);

        assertEquals(1, obstacles.size());
        MarkedObstacle obstacle = obstacles.getFirst();

        assertEquals(ObstacleRisk.HIGH, obstacle.getObstacleRisk());
        assertEquals(true, (obstacle.getHorizontalDistance() <= 200));
        assertEquals(true, (obstacle.getVerticalDistance() <= 75));
    }

    @Test 
    public void testHighRiskObstacleLowHorizontalLowVertical() {
        String csv = "-87.681418,30.290059,999,01-099999,U,US,AL,GULF SHORES,30 17 24.21N,087 40 53.10W,30.290059,-87.681418,TOWER,1,130,150,N,4,D,N,2026ASO00001OE,A,2026223";
        generateData(csv);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.290059, -87.682052, 100, 1000);

        assertEquals(1, obstacles.size());
        MarkedObstacle obstacle = obstacles.getFirst();

        assertEquals(ObstacleRisk.HIGH, obstacle.getObstacleRisk());
        assertEquals(true, (obstacle.getHorizontalDistance() <= 200));
        assertEquals(true, (obstacle.getVerticalDistance() <= 75));
    }

    @Test 
    public void testMediumRiskObstacleCloseHorizontal() {
        String csv = "-87.681418,30.290059,999,01-099999,U,US,AL,GULF SHORES,30 17 24.21N,087 40 53.10W,30.290059,-87.681418,TOWER,1,180,200,N,4,D,N,2026ASO00001OE,A,2026223";
        generateData(csv);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.290059, -87.682052, 100, 1000);

        assertEquals(1, obstacles.size());
        MarkedObstacle obstacle = obstacles.getFirst();

        assertEquals(ObstacleRisk.MEDIUM, obstacle.getObstacleRisk());
        assertEquals(true, (obstacle.getHorizontalDistance() < 500));
        assertEquals(true, (obstacle.getVerticalDistance() > 75));
        assertEquals(true, (obstacle.getVerticalDistance() < 200));
    }

    @Test 
    public void testMediumRiskObstacleCloseVertical() {
        String csv = "-87.679833,30.290059,999,01-099999,U,US,AL,GULF SHORES,30 17 24.21N,087 40 47.40W,30.290059,-87.679833,TOWER,1,90,110,N,4,D,N,2026ASO00001OE,A,2026223";
        generateData(csv);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.290059, -87.682052, 100, 1000);

        assertEquals(1, obstacles.size());
        MarkedObstacle obstacle = obstacles.getFirst();

        assertEquals(ObstacleRisk.MEDIUM, obstacle.getObstacleRisk());
        assertEquals(true, (obstacle.getHorizontalDistance() > 500));
        assertEquals(true, (obstacle.getHorizontalDistance() < 1000));
        assertEquals(true, (obstacle.getVerticalDistance() < 75));
    }

    @Test
    public void testLowRiskObstacle() {
        String csv = "-87.679833,30.290059,999,01-099999,U,US,AL,GULF SHORES,30 17 24.21N,087 40 47.40W,30.290059,-87.679833,TOWER,1,180,200,N,4,D,N,2026ASO00001OE,A,2026223";
        generateData(csv);
        ArrayList<MarkedObstacle> obstacles = Obstacles.getNearbyObstaclesWithinRange( 30.290059, -87.682052, 100, 1000);

        assertEquals(1, obstacles.size());
        MarkedObstacle obstacle = obstacles.getFirst();
        
        assertEquals(ObstacleRisk.LOW, obstacle.getObstacleRisk());
        assertEquals(true, (obstacle.getHorizontalDistance() > 500));
        assertEquals(true, (obstacle.getHorizontalDistance() < 1000));
        assertEquals(true, (obstacle.getVerticalDistance() > 75));
        assertEquals(true, (obstacle.getVerticalDistance() < 200));
    }
    

}
