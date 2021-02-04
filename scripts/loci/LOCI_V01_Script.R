

## Load the required packages
library(readxl)
library(readr)
library(dplyr)
library(ggplot2)

## set the directory where the data is located
directory <- ("~/Downloads")
flight_data_file <- ("flight_4.csv")

## set the critical angle of attack and pro-spin force limits
AOA_Crit <- (15)
Pro_Spin_Lim <- (4)

setwd(directory)

## load a sample data file for the Aircraft
Flight_Data <- read_csv(flight_data_file)

## compute derived parameters

##Offsets Since Beginning of File
Flight_Data$Offset <- seq.int(nrow(Flight_Data))
## Temperature Ratio (theta)
Flight_Data$Temperature_Ratio <- ((273 + Flight_Data$OAT) / 288)
## Static Pressure Ratio (delta)
Flight_Data$Pressure_Ratio <- (Flight_Data$BaroA / 29.92)
## Density Ratio (sigma)
Flight_Data$density_ratio <-
  (Flight_Data$Pressure_Ratio / Flight_Data$Temperature_Ratio)
## True Airspeed (knots)
Flight_Data$True_Airspeed_Computed <-
  (Flight_Data$IAS * (1 / (sqrt(
    Flight_Data$density_ratio
  ))))
## True Airspeed (ft/min)
Flight_Data$TAS_ft_per_minute <-
  (Flight_Data$True_Airspeed_Computed * (6076 / 60))
## Vertical Speed [Geometric] (ft/min)
Flight_Data$Vertical_Speed_Geometric <-
  (Flight_Data$VSpd * (1 / (sqrt(
    Flight_Data$density_ratio
  ))))
## Flight Path Angle (deg)
Flight_Data$flight_path_angle <-
  asin(Flight_Data$Vertical_Speed_Geometric / Flight_Data$TAS_ft_per_minute)
Flight_Data$flight_path_angle <-
  (Flight_Data$flight_path_angle * (180 / pi))
## Angle of Attack (deg)
Flight_Data$Angle_of_Attack_Simple <-
  (Flight_Data$Pitch - Flight_Data$flight_path_angle)
## Yaw Rate (deg/sec) **This assumes a 1hz sample rate between offsets. If this assumption is not true, correction is required
Flight_Data$Yaw_Rate <- (Flight_Data$HDG - lag(Flight_Data$HDG))
Flight_Data$Yaw_Rate <-
  180 - abs(180 - abs(Flight_Data$HDG - lag(Flight_Data$HDG)) %% 360)
## % to Critical Angle of Attack
Flight_Data$Stall_Prob <-
  pmin(((
    abs(Flight_Data$Angle_of_Attack_Simple / AOA_Crit)
  ) * 100), 100, na.rm = TRUE)
## Compute Pro-Spin
Flight_Data$Roll_Comp <- (Flight_Data$Roll * (pi / 180))
Flight_Data$Yaw_Comp <- (Flight_Data$Yaw_Rate * (pi / 180))
Flight_Data$Vr_Comp <-
  ((Flight_Data$TAS_ft_per_minute / 60) * Flight_Data$Yaw_Comp)
Flight_Data$CT_Comp <- sin(Flight_Data$Roll_Comp) * 32.2
Flight_Data$Cord_Comp <-
  abs(Flight_Data$CT_Comp - Flight_Data$Vr_Comp) * 100

Flight_Data$Pro_Spin <-
  pmin((Flight_Data$Cord_Comp / Pro_Spin_Lim), 100)

##Probability of Upset / LOC-I
Flight_Data$LOCI <- (Flight_Data$Stall_Prob * Flight_Data$Pro_Spin) / 100

notvars <-
  names(Flight_Data) %in% c(
    "Temperature_Ratio",
    "Pressure_Ratio",
    "density_ratio",
    "True_Airspeed_Computed",
    "TAS_ft_per_minute",
    "Vertical_Speed_Geometric",
    "Offset",
    "flight_path_angle",
    "Angle_of_Attack_Simple",
    "Yaw_Rate",
    "Roll_Comp",
    "Yaw_Comp",
    "Vr_Comp",
    "CT_Comp",
    "Cord_Comp"
  )

Flight_Data_Limited <- Flight_Data[!notvars]

##Save a couple of files.

write.csv(Flight_Data_Limited, file = "output_filepath_and_name.csv", col.names = TRUE)
write.csv(Flight_Data$LOCI, file = "output_filepath_and_name.csv", col.names = TRUE)
