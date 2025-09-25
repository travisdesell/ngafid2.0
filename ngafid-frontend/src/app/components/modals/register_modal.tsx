// ngafid-frontend/src/app/components/modals/register_modal.tsx
import { X, Loader2Icon, AlertCircleIcon } from "lucide-react";
import { motion } from "motion/react";
import { Card, CardContent, CardHeader, CardDescription, CardFooter, CardTitle, CardAction } from "@/components/ui/card"
import { Button } from '@/components/ui/button';
import { Input } from "@/components/ui/input";
import { Label } from '@/components/ui/label';
import React, { use, useEffect } from "react";
import type { ModalProps } from "./types";
import { Alert, AlertTitle, AlertDescription } from "../ui/alert";
import { Separator } from "@/components/ui/separator";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select";
import { RadioGroup, RadioGroupItem } from "../ui/radio-group";
import { Accordion, AccordionItem, AccordionContent, AccordionTrigger } from "../ui/accordion";
import ErrorModal, { ModalDataError } from "./error_modal";

export default function RegisterModal({ setModal }: ModalProps) {

    const close = () => setModal(undefined);


    const [errorMessage, setErrorMessage] = React.useState("");

    const [email, setEmail] = React.useState("");
    const [password, setPassword] = React.useState("");

    const [firstName, setFirstName] = React.useState("");
    const [lastName, setLastName] = React.useState("");
    const [phoneNumber, setPhoneNumber] = React.useState("");
    const [address, setAddress] = React.useState("");
    const [country, setCountry] = React.useState("");
    const [state, setState] = React.useState("");
    const [city, setCity] = React.useState("");
    const [zipCode, setZipCode] = React.useState("");

    const [isManagingNewFleet, setIsManagingNewFleet] = React.useState(true);
    const [newFleetName, setNewFleetName] = React.useState("");
    const [selectedFleetName, setSelectedFleetName] = React.useState("");
    const [fleetList, setFleetList] = React.useState<string[]>([]);

    const [isLoading, setIsLoading] = React.useState(false);

    useEffect(() => {

        //Fetch the fleet list from the API
        const fetchFleetList = async () => {

            fetch("/api/fleet/names")
                .then((response) => response.json())
                .then((data) => setFleetList(data))
                .catch((error) => {
                    setModal(ErrorModal, {title : "Error fetching fleet list", message: error.toString()} as ModalDataError);
                }
            );

        };

        fetchFleetList();
    }, []);


    const submitRegister = () => {

        console.log("Attempting to submit registration....");

        //Flag as loading
        setIsLoading(true);

        try {

            //Email is empty, exit
            const emailEmpty = (email.trim().length === 0);
            if (emailEmpty)
                throw new Error("Preventing registration submission - Email is required.");

            //Password is empty, exit
            const passwordEmpty = (password.trim().length === 0);
            if (passwordEmpty)
                throw new Error("Preventing registration submission - Password is required.");

            //Managing new fleet...
            if (isManagingNewFleet) {

                //...Fleet name is empty, exit
                const newFleetNameEmpty = (newFleetName.trim().length === 0);
                if (newFleetNameEmpty)
                    throw new Error("Preventing registration submission - Fleet Name is required when creating a new fleet.");

            //Otherwise...
            } else {

                //...Selected fleet name is empty, exit
                const selectedFleetNameEmpty = (selectedFleetName.trim().length === 0);
                if (selectedFleetNameEmpty)
                    throw new Error("Preventing registration submission - You must select a fleet to manage.");

            }

            const accountType = (isManagingNewFleet ? "newFleet" : "existingFleet");
            const fleetName = (isManagingNewFleet ? newFleetName : selectedFleetName);
            const form = new URLSearchParams({
                'email': email,
                'password': password,
                'firstName': firstName,
                'lastName': lastName,
                'phoneNumber': phoneNumber,
                'address': address,
                'country': country,
                'state': state,
                'city': city,
                'zipCode': zipCode,
                'accountType': accountType,
                'fleetName': fleetName
            });

            fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: form,
            })
            .then(response => response.json())
            .then(data => {

                //Clear password field in UI state
                setPassword("");

                //Got an error, show error modal
                if (data.errorTitle) {
                    setModal(ErrorModal, {title : data.errorTitle, message:data.errorMessage} as ModalDataError);
                    return;
                }

                //Success, close the modal
                close();

                //Managing fleet, go to Summary page
                if (isManagingNewFleet)
                    window.location.replace("/app/summary");

                //Otherwise, go to the Waiting page
                else
                    window.location.replace("/app/waiting");

            })
            .catch((error) => {
                console.error("Error during registration fetch:", error);
                setModal(ErrorModal, {title : "Error during registration", message: error.toString()} as ModalDataError);
            });
            
        } catch (error) {
            console.error(error);
            if (error instanceof Error) {
                setErrorMessage(error.message);
            } else {
                setErrorMessage("An unknown error occurred.");
            }
            return;
        }

           setIsLoading(false);

    };


    const submitDisabled = (
        isLoading
        || email.trim().length === 0
        || password.trim().length === 0
        || (isManagingNewFleet && newFleetName.trim().length === 0)
        || (!isManagingNewFleet && selectedFleetName.trim().length === 0)
    );


    const render = () => {

        return (
            <motion.div
                initial={{ scale: 0, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0, opacity: 0 }}
                className="w-full h-full"
            >
                <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 max-h-[80vh] overflow-y-auto">
                    <CardHeader className="grid gap-2">

                        <div className="grid gap-2">
                            <CardTitle>Create an NGAFID account</CardTitle>
                            <CardDescription>
                                Enter an email and password to create your account.
                            </CardDescription>
                        </div>

                        <CardAction>
                            <Button variant="link" onClick={close} disabled={isLoading}>
                                <X />
                            </Button>
                        </CardAction>

                    </CardHeader>
                    <CardContent>
                        <form>
                            <div className="flex flex-col gap-6">
                                {
                                    errorMessage
                                    &&
                                    <Alert variant="destructive">
                                        <AlertCircleIcon size={16} />
                                        <AlertTitle>Error logging in.</AlertTitle>
                                        <AlertDescription>
                                            {errorMessage}
                                        </AlertDescription>
                                    </Alert>
                                }

                                {/* -- Required Info -- */}
                                <div className="grid gap-2">
                                    <Label htmlFor="email">Email</Label>
                                    <Input
                                        id="email"
                                        type="email"
                                        placeholder="name@example.com (required)"
                                        required
                                        onChange={(e) => setEmail(e.target.value)}
                                        value={email}
                                    />
                                </div>
                                <div className="grid gap-2">
                                    <Label htmlFor="password">Password</Label>
                                    <Input
                                        id="password"
                                        type="password"
                                        placeholder="•••••••• (required)"
                                        required
                                        onChange={(e) => setPassword(e.target.value)}
                                        value={password}
                                    />
                                </div>
                                {/* <Separator /> */}

                                {/* -- Optional Info -- */}
                                <Accordion type="single" collapsible>
                                    <AccordionItem value="optionalInfo" className="border-transparent">
                                        <AccordionTrigger className="mb-6 mt-4">
                                            <hr className='border-b-1 border-gray-500/25 flex flex-1 mx-4'/>
                                            Optional Contact Information
                                            <hr className='border-b-1 border-gray-500/25 flex flex-1 mx-4'/>
                                        </AccordionTrigger>
                                        <AccordionContent className="flex flex-col gap-6 px-4">
                                            <div className="flex flex-row gap-4">

                                                {/* First Name */}
                                                <div className="grid gap-2 w-full">
                                                    <Label htmlFor="firstName">First Name</Label>
                                                    <Input
                                                        id="firstName"
                                                        type="text"
                                                        placeholder="First Name (optional)"
                                                        onChange={(e) => setFirstName(e.target.value)}
                                                        value={firstName}
                                                    />
                                                </div>

                                                {/* Last Name */}
                                                <div className="grid gap-2 w-full">
                                                    <Label htmlFor="lastName">Last Name</Label>
                                                    <Input
                                                        id="lastName"
                                                        type="text"
                                                        placeholder="Last Name (optional)"
                                                        onChange={(e) => setLastName(e.target.value)}
                                                        value={lastName}
                                                    />
                                                </div>
                                            </div>
                                            <div className="grid gap-2 w-full">
                                                <Label htmlFor="phoneNumber">Phone Number</Label>
                                                <Input
                                                    id="phoneNumber"
                                                    type="text"
                                                    placeholder="Phone Number (optional)"
                                                    onChange={(e) => setPhoneNumber(e.target.value)}
                                                    value={phoneNumber}
                                                />
                                            </div>
                                            <div className="grid gap-2 w-full">
                                                <Label htmlFor="address">Address</Label>
                                                <Input
                                                    id="address"
                                                    type="text"
                                                    placeholder="Address (optional)"
                                                    onChange={(e) => setAddress(e.target.value)}
                                                    value={address}
                                                />
                                            </div>
                                            <div className="flex flex-row gap-4">
                                                <div className="grid gap-2 w-full">
                                                    <Label htmlFor="country">Country</Label>
                                                    <Select onValueChange={(value) => setCountry(value)}>
                                                        <SelectTrigger id="country" className="w-full">
                                                            <SelectValue className="text-muted-foreground" placeholder="Select a country (optional)" />
                                                        </SelectTrigger>
                                                        <SelectContent className="max-h-[300px] overflow-y-auto">
                                                            <SelectItem value="USA">United States</SelectItem>
                                                            <Separator className="my-3 w-[calc(100%-1rem)] ml-2" />
                                                            <SelectItem value="AFG">Afghanistan</SelectItem>
                                                            <SelectItem value="ALA">Åland Islands</SelectItem>
                                                            <SelectItem value="ALB">Albania</SelectItem>
                                                            <SelectItem value="DZA">Algeria</SelectItem>
                                                            <SelectItem value="ASM">American Samoa</SelectItem>
                                                            <SelectItem value="AND">Andorra</SelectItem>
                                                            <SelectItem value="AGO">Angola</SelectItem>
                                                            <SelectItem value="AIA">Anguilla</SelectItem>
                                                            <SelectItem value="ATA">Antarctica</SelectItem>
                                                            <SelectItem value="ATG">Antigua and Barbuda</SelectItem>
                                                            <SelectItem value="ARG">Argentina</SelectItem>
                                                            <SelectItem value="ARM">Armenia</SelectItem>
                                                            <SelectItem value="ABW">Aruba</SelectItem>
                                                            <SelectItem value="AUS">Australia</SelectItem>
                                                            <SelectItem value="AUT">Austria</SelectItem>
                                                            <SelectItem value="AZE">Azerbaijan</SelectItem>
                                                            <SelectItem value="BHS">Bahamas</SelectItem>
                                                            <SelectItem value="BHR">Bahrain</SelectItem>
                                                            <SelectItem value="BGD">Bangladesh</SelectItem>
                                                            <SelectItem value="BRB">Barbados</SelectItem>
                                                            <SelectItem value="BLR">Belarus</SelectItem>
                                                            <SelectItem value="BEL">Belgium</SelectItem>
                                                            <SelectItem value="BLZ">Belize</SelectItem>
                                                            <SelectItem value="BEN">Benin</SelectItem>
                                                            <SelectItem value="BMU">Bermuda</SelectItem>
                                                            <SelectItem value="BTN">Bhutan</SelectItem>
                                                            <SelectItem value="BOL">Bolivia, Plurinational State of</SelectItem>
                                                            <SelectItem value="BES">Bonaire, Sint Eustatius and Saba</SelectItem>
                                                            <SelectItem value="BIH">Bosnia and Herzegovina</SelectItem>
                                                            <SelectItem value="BWA">Botswana</SelectItem>
                                                            <SelectItem value="BVT">Bouvet Island</SelectItem>
                                                            <SelectItem value="BRA">Brazil</SelectItem>
                                                            <SelectItem value="IOT">British Indian Ocean Territory</SelectItem>
                                                            <SelectItem value="BRN">Brunei Darussalam</SelectItem>
                                                            <SelectItem value="BGR">Bulgaria</SelectItem>
                                                            <SelectItem value="BFA">Burkina Faso</SelectItem>
                                                            <SelectItem value="BDI">Burundi</SelectItem>
                                                            <SelectItem value="KHM">Cambodia</SelectItem>
                                                            <SelectItem value="CMR">Cameroon</SelectItem>
                                                            <SelectItem value="CAN">Canada</SelectItem>
                                                            <SelectItem value="CPV">Cape Verde</SelectItem>
                                                            <SelectItem value="CYM">Cayman Islands</SelectItem>
                                                            <SelectItem value="CAF">Central African Republic</SelectItem>
                                                            <SelectItem value="TCD">Chad</SelectItem>
                                                            <SelectItem value="CHL">Chile</SelectItem>
                                                            <SelectItem value="CHN">China</SelectItem>
                                                            <SelectItem value="CXR">Christmas Island</SelectItem>
                                                            <SelectItem value="CCK">Cocos (Keeling) Islands</SelectItem>
                                                            <SelectItem value="COL">Colombia</SelectItem>
                                                            <SelectItem value="COM">Comoros</SelectItem>
                                                            <SelectItem value="COG">Congo</SelectItem>
                                                            <SelectItem value="COD">Congo, the Democratic Republic of the</SelectItem>
                                                            <SelectItem value="COK">Cook Islands</SelectItem>
                                                            <SelectItem value="CRI">Costa Rica</SelectItem>
                                                            <SelectItem value="CIV">Côte d&#39;Ivoire</SelectItem>
                                                            <SelectItem value="HRV">Croatia</SelectItem>
                                                            <SelectItem value="CUB">Cuba</SelectItem>
                                                            <SelectItem value="CUW">Curaçao</SelectItem>
                                                            <SelectItem value="CYP">Cyprus</SelectItem>
                                                            <SelectItem value="CZE">Czech Republic</SelectItem>
                                                            <SelectItem value="DNK">Denmark</SelectItem>
                                                            <SelectItem value="DJI">Djibouti</SelectItem>
                                                            <SelectItem value="DMA">Dominica</SelectItem>
                                                            <SelectItem value="DOM">Dominican Republic</SelectItem>
                                                            <SelectItem value="ECU">Ecuador</SelectItem>
                                                            <SelectItem value="EGY">Egypt</SelectItem>
                                                            <SelectItem value="SLV">El Salvador</SelectItem>
                                                            <SelectItem value="GNQ">Equatorial Guinea</SelectItem>
                                                            <SelectItem value="ERI">Eritrea</SelectItem>
                                                            <SelectItem value="EST">Estonia</SelectItem>
                                                            <SelectItem value="ETH">Ethiopia</SelectItem>
                                                            <SelectItem value="FLK">Falkland Islands (Malvinas)</SelectItem>
                                                            <SelectItem value="FRO">Faroe Islands</SelectItem>
                                                            <SelectItem value="FJI">Fiji</SelectItem>
                                                            <SelectItem value="FIN">Finland</SelectItem>
                                                            <SelectItem value="FRA">France</SelectItem>
                                                            <SelectItem value="GUF">French Guiana</SelectItem>
                                                            <SelectItem value="PYF">French Polynesia</SelectItem>
                                                            <SelectItem value="ATF">French Southern Territories</SelectItem>
                                                            <SelectItem value="GAB">Gabon</SelectItem>
                                                            <SelectItem value="GMB">Gambia</SelectItem>
                                                            <SelectItem value="GEO">Georgia</SelectItem>
                                                            <SelectItem value="DEU">Germany</SelectItem>
                                                            <SelectItem value="GHA">Ghana</SelectItem>
                                                            <SelectItem value="GIB">Gibraltar</SelectItem>
                                                            <SelectItem value="GRC">Greece</SelectItem>
                                                            <SelectItem value="GRL">Greenland</SelectItem>
                                                            <SelectItem value="GRD">Grenada</SelectItem>
                                                            <SelectItem value="GLP">Guadeloupe</SelectItem>
                                                            <SelectItem value="GUM">Guam</SelectItem>
                                                            <SelectItem value="GTM">Guatemala</SelectItem>
                                                            <SelectItem value="GGY">Guernsey</SelectItem>
                                                            <SelectItem value="GIN">Guinea</SelectItem>
                                                            <SelectItem value="GNB">Guinea-Bissau</SelectItem>
                                                            <SelectItem value="GUY">Guyana</SelectItem>
                                                            <SelectItem value="HTI">Haiti</SelectItem>
                                                            <SelectItem value="HMD">Heard Island and McDonald Islands</SelectItem>
                                                            <SelectItem value="VAT">Holy See (Vatican City State)</SelectItem>
                                                            <SelectItem value="HND">Honduras</SelectItem>
                                                            <SelectItem value="HKG">Hong Kong</SelectItem>
                                                            <SelectItem value="HUN">Hungary</SelectItem>
                                                            <SelectItem value="ISL">Iceland</SelectItem>
                                                            <SelectItem value="IND">India</SelectItem>
                                                            <SelectItem value="IDN">Indonesia</SelectItem>
                                                            <SelectItem value="IRN">Iran, Islamic Republic of</SelectItem>
                                                            <SelectItem value="IRQ">Iraq</SelectItem>
                                                            <SelectItem value="IRL">Ireland</SelectItem>
                                                            <SelectItem value="IMN">Isle of Man</SelectItem>
                                                            <SelectItem value="ISR">Israel</SelectItem>
                                                            <SelectItem value="ITA">Italy</SelectItem>
                                                            <SelectItem value="JAM">Jamaica</SelectItem>
                                                            <SelectItem value="JPN">Japan</SelectItem>
                                                            <SelectItem value="JEY">Jersey</SelectItem>
                                                            <SelectItem value="JOR">Jordan</SelectItem>
                                                            <SelectItem value="KAZ">Kazakhstan</SelectItem>
                                                            <SelectItem value="KEN">Kenya</SelectItem>
                                                            <SelectItem value="KIR">Kiribati</SelectItem>
                                                            <SelectItem value="PRK">Korea, Democratic People&#39;s Republic of</SelectItem>
                                                            <SelectItem value="KOR">Korea, Republic of</SelectItem>
                                                            <SelectItem value="KWT">Kuwait</SelectItem>
                                                            <SelectItem value="KGZ">Kyrgyzstan</SelectItem>
                                                            <SelectItem value="LAO">Lao People&#39;s Democratic Republic</SelectItem>
                                                            <SelectItem value="LVA">Latvia</SelectItem>
                                                            <SelectItem value="LBN">Lebanon</SelectItem>
                                                            <SelectItem value="LSO">Lesotho</SelectItem>
                                                            <SelectItem value="LBR">Liberia</SelectItem>
                                                            <SelectItem value="LBY">Libya</SelectItem>
                                                            <SelectItem value="LIE">Liechtenstein</SelectItem>
                                                            <SelectItem value="LTU">Lithuania</SelectItem>
                                                            <SelectItem value="LUX">Luxembourg</SelectItem>
                                                            <SelectItem value="MAC">Macao</SelectItem>
                                                            <SelectItem value="MKD">Macedonia, the former Yugoslav Republic of</SelectItem>
                                                            <SelectItem value="MDG">Madagascar</SelectItem>
                                                            <SelectItem value="MWI">Malawi</SelectItem>
                                                            <SelectItem value="MYS">Malaysia</SelectItem>
                                                            <SelectItem value="MDV">Maldives</SelectItem>
                                                            <SelectItem value="MLI">Mali</SelectItem>
                                                            <SelectItem value="MLT">Malta</SelectItem>
                                                            <SelectItem value="MHL">Marshall Islands</SelectItem>
                                                            <SelectItem value="MTQ">Martinique</SelectItem>
                                                            <SelectItem value="MRT">Mauritania</SelectItem>
                                                            <SelectItem value="MUS">Mauritius</SelectItem>
                                                            <SelectItem value="MYT">Mayotte</SelectItem>
                                                            <SelectItem value="MEX">Mexico</SelectItem>
                                                            <SelectItem value="FSM">Micronesia, Federated States of</SelectItem>
                                                            <SelectItem value="MDA">Moldova, Republic of</SelectItem>
                                                            <SelectItem value="MCO">Monaco</SelectItem>
                                                            <SelectItem value="MNG">Mongolia</SelectItem>
                                                            <SelectItem value="MNE">Montenegro</SelectItem>
                                                            <SelectItem value="MSR">Montserrat</SelectItem>
                                                            <SelectItem value="MAR">Morocco</SelectItem>
                                                            <SelectItem value="MOZ">Mozambique</SelectItem>
                                                            <SelectItem value="MMR">Myanmar</SelectItem>
                                                            <SelectItem value="NAM">Namibia</SelectItem>
                                                            <SelectItem value="NRU">Nauru</SelectItem>
                                                            <SelectItem value="NPL">Nepal</SelectItem>
                                                            <SelectItem value="NLD">Netherlands</SelectItem>
                                                            <SelectItem value="NCL">New Caledonia</SelectItem>
                                                            <SelectItem value="NZL">New Zealand</SelectItem>
                                                            <SelectItem value="NIC">Nicaragua</SelectItem>
                                                            <SelectItem value="NER">Niger</SelectItem>
                                                            <SelectItem value="NGA">Nigeria</SelectItem>
                                                            <SelectItem value="NIU">Niue</SelectItem>
                                                            <SelectItem value="NFK">Norfolk Island</SelectItem>
                                                            <SelectItem value="MNP">Northern Mariana Islands</SelectItem>
                                                            <SelectItem value="NOR">Norway</SelectItem>
                                                            <SelectItem value="OMN">Oman</SelectItem>
                                                            <SelectItem value="PAK">Pakistan</SelectItem>
                                                            <SelectItem value="PLW">Palau</SelectItem>
                                                            <SelectItem value="PSE">Palestinian Territory, Occupied</SelectItem>
                                                            <SelectItem value="PAN">Panama</SelectItem>
                                                            <SelectItem value="PNG">Papua New Guinea</SelectItem>
                                                            <SelectItem value="PRY">Paraguay</SelectItem>
                                                            <SelectItem value="PER">Peru</SelectItem>
                                                            <SelectItem value="PHL">Philippines</SelectItem>
                                                            <SelectItem value="PCN">Pitcairn</SelectItem>
                                                            <SelectItem value="POL">Poland</SelectItem>
                                                            <SelectItem value="PRT">Portugal</SelectItem>
                                                            <SelectItem value="PRI">Puerto Rico</SelectItem>
                                                            <SelectItem value="QAT">Qatar</SelectItem>
                                                            <SelectItem value="REU">Réunion</SelectItem>
                                                            <SelectItem value="ROU">Romania</SelectItem>
                                                            <SelectItem value="RUS">Russian Federation</SelectItem>
                                                            <SelectItem value="RWA">Rwanda</SelectItem>
                                                            <SelectItem value="BLM">Saint Barthélemy</SelectItem>
                                                            <SelectItem value="SHN">Saint Helena, Ascension and Tristan da Cunha</SelectItem>
                                                            <SelectItem value="KNA">Saint Kitts and Nevis</SelectItem>
                                                            <SelectItem value="LCA">Saint Lucia</SelectItem>
                                                            <SelectItem value="MAF">Saint Martin (French part)</SelectItem>
                                                            <SelectItem value="SPM">Saint Pierre and Miquelon</SelectItem>
                                                            <SelectItem value="VCT">Saint Vincent and the Grenadines</SelectItem>
                                                            <SelectItem value="WSM">Samoa</SelectItem>
                                                            <SelectItem value="SMR">San Marino</SelectItem>
                                                            <SelectItem value="STP">Sao Tome and Principe</SelectItem>
                                                            <SelectItem value="SAU">Saudi Arabia</SelectItem>
                                                            <SelectItem value="SEN">Senegal</SelectItem>
                                                            <SelectItem value="SRB">Serbia</SelectItem>
                                                            <SelectItem value="SYC">Seychelles</SelectItem>
                                                            <SelectItem value="SLE">Sierra Leone</SelectItem>
                                                            <SelectItem value="SGP">Singapore</SelectItem>
                                                            <SelectItem value="SXM">Sint Maarten (Dutch part)</SelectItem>
                                                            <SelectItem value="SVK">Slovakia</SelectItem>
                                                            <SelectItem value="SVN">Slovenia</SelectItem>
                                                            <SelectItem value="SLB">Solomon Islands</SelectItem>
                                                            <SelectItem value="SOM">Somalia</SelectItem>
                                                            <SelectItem value="ZAF">South Africa</SelectItem>
                                                            <SelectItem value="SGS">South Georgia and the South Sandwich Islands</SelectItem>
                                                            <SelectItem value="SSD">South Sudan</SelectItem>
                                                            <SelectItem value="ESP">Spain</SelectItem>
                                                            <SelectItem value="LKA">Sri Lanka</SelectItem>
                                                            <SelectItem value="SDN">Sudan</SelectItem>
                                                            <SelectItem value="SUR">Suriname</SelectItem>
                                                            <SelectItem value="SJM">Svalbard and Jan Mayen</SelectItem>
                                                            <SelectItem value="SWZ">Swaziland</SelectItem>
                                                            <SelectItem value="SWE">Sweden</SelectItem>
                                                            <SelectItem value="CHE">Switzerland</SelectItem>
                                                            <SelectItem value="SYR">Syrian Arab Republic</SelectItem>
                                                            <SelectItem value="TWN">Taiwan, Province of China</SelectItem>
                                                            <SelectItem value="TJK">Tajikistan</SelectItem>
                                                            <SelectItem value="TZA">Tanzania, United Republic of</SelectItem>
                                                            <SelectItem value="THA">Thailand</SelectItem>
                                                            <SelectItem value="TLS">Timor-Leste</SelectItem>
                                                            <SelectItem value="TGO">Togo</SelectItem>
                                                            <SelectItem value="TKL">Tokelau</SelectItem>
                                                            <SelectItem value="TON">Tonga</SelectItem>
                                                            <SelectItem value="TTO">Trinidad and Tobago</SelectItem>
                                                            <SelectItem value="TUN">Tunisia</SelectItem>
                                                            <SelectItem value="TUR">Turkey</SelectItem>
                                                            <SelectItem value="TKM">Turkmenistan</SelectItem>
                                                            <SelectItem value="TCA">Turks and Caicos Islands</SelectItem>
                                                            <SelectItem value="TUV">Tuvalu</SelectItem>
                                                            <SelectItem value="UGA">Uganda</SelectItem>
                                                            <SelectItem value="UKR">Ukraine</SelectItem>
                                                            <SelectItem value="ARE">United Arab Emirates</SelectItem>
                                                            <SelectItem value="GBR">United Kingdom</SelectItem>
                                                            <SelectItem value="UMI">United States Minor Outlying Islands</SelectItem>
                                                            <SelectItem value="URY">Uruguay</SelectItem>
                                                            <SelectItem value="UZB">Uzbekistan</SelectItem>
                                                            <SelectItem value="VUT">Vanuatu</SelectItem>
                                                            <SelectItem value="VEN">Venezuela, Bolivarian Republic of</SelectItem>
                                                            <SelectItem value="VNM">Viet Nam</SelectItem>
                                                            <SelectItem value="VGB">Virgin Islands, British</SelectItem>
                                                            <SelectItem value="VIR">Virgin Islands, U.S.</SelectItem>
                                                            <SelectItem value="WLF">Wallis and Futuna</SelectItem>
                                                            <SelectItem value="ESH">Western Sahara</SelectItem>
                                                            <SelectItem value="YEM">Yemen</SelectItem>
                                                            <SelectItem value="ZMB">Zambia</SelectItem>
                                                            <SelectItem value="ZWE">Zimbabwe</SelectItem>
                                                        </SelectContent>
                                                    </Select>
                                                </div>
                                                {
                                                    //State selection if country is USA
                                                    (country === "USA")
                                                    &&
                                                    <div className="grid gap-2 w-full">
                                                        <Label htmlFor="state">State</Label>
                                                        <Select onValueChange={(value) => setState(value)}>
                                                            <SelectTrigger id="state" className="w-full">
                                                                <SelectValue className="text-muted-foreground" placeholder="Select a state (optional)" />
                                                            </SelectTrigger>
                                                            <SelectContent className="max-h-[300px] overflow-y-auto">
                                                                <SelectItem value="AL">Alabama</SelectItem>
                                                                <SelectItem value="AK">Alaska</SelectItem>
                                                                <SelectItem value="AZ">Arizona</SelectItem>
                                                                <SelectItem value="AR">Arkansas</SelectItem>
                                                                <SelectItem value="CA">California</SelectItem>
                                                                <SelectItem value="CO">Colorado</SelectItem>
                                                                <SelectItem value="CT">Connecticut</SelectItem>
                                                                <SelectItem value="DE">Delaware</SelectItem>
                                                                <SelectItem value="FL">Florida</SelectItem>
                                                                <SelectItem value="GA">Georgia</SelectItem>
                                                                <SelectItem value="HI">Hawaii</SelectItem>
                                                                <SelectItem value="ID">Idaho</SelectItem>
                                                                <SelectItem value="IL">Illinois</SelectItem>
                                                                <SelectItem value="IN">Indiana</SelectItem>
                                                                <SelectItem value="IA">Iowa</SelectItem>
                                                                <SelectItem value="KS">Kansas</SelectItem>
                                                                <SelectItem value="KY">Kentucky</SelectItem>
                                                                <SelectItem value="LA">Louisiana</SelectItem>
                                                                <SelectItem value="ME">Maine</SelectItem>
                                                                <SelectItem value="MD">Maryland</SelectItem>
                                                                <SelectItem value="MA">Massachusetts</SelectItem>
                                                                <SelectItem value="MI">Michigan</SelectItem>
                                                                <SelectItem value="MN">Minnesota</SelectItem>
                                                                <SelectItem value="MS">Mississippi</SelectItem>
                                                                <SelectItem value="MO">Missouri</SelectItem>
                                                                <SelectItem value="MT">Montana</SelectItem>
                                                                <SelectItem value="NE">Nebraska</SelectItem>
                                                                <SelectItem value="NV">Nevada</SelectItem>
                                                                <SelectItem value="NH">New Hampshire</SelectItem>
                                                                <SelectItem value="NJ">New Jersey</SelectItem>
                                                                <SelectItem value="NM">New Mexico</SelectItem>
                                                                <SelectItem value="NY">New York</SelectItem>
                                                                <SelectItem value="NC">North Carolina</SelectItem>
                                                                <SelectItem value="ND">North Dakota</SelectItem>
                                                                <SelectItem value="OH">Ohio</SelectItem>
                                                                <SelectItem value="OK">Oklahoma</SelectItem>
                                                                <SelectItem value="OR">Oregon</SelectItem>
                                                                <SelectItem value="PA">Pennsylvania</SelectItem>
                                                                <SelectItem value="RI">Rhode Island</SelectItem>
                                                                <SelectItem value="SC">South Carolina</SelectItem>
                                                                <SelectItem value="SD">South Dakota</SelectItem>
                                                                <SelectItem value="TN">Tennessee</SelectItem>
                                                                <SelectItem value="TX">Texas</SelectItem>
                                                                <SelectItem value="UT">Utah</SelectItem>
                                                                <SelectItem value="VT">Vermont</SelectItem>
                                                                <SelectItem value="VA">Virginia</SelectItem>
                                                                <SelectItem value="WA">Washington</SelectItem>
                                                                <SelectItem value="WV">West Virginia</SelectItem>
                                                                <SelectItem value="WI">Wisconsin</SelectItem>
                                                                <SelectItem value="WY">Wyoming</SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                    </div>
                                                }
                                            </div>
                                            <div className="flex flex-row gap-4">

                                                {/* City */}
                                                <div className="grid gap-2 w-full">
                                                    <Label htmlFor="city">City</Label>
                                                    <Input
                                                        id="city"
                                                        type="text"
                                                        placeholder="City (optional)"
                                                        onChange={(e) => setCity(e.target.value)}
                                                        value={city}
                                                    />
                                                </div>

                                                {/* Zip Code */}
                                                <div className="grid gap-2 w-full">
                                                    <Label htmlFor="zipCode">Zip Code</Label>
                                                    <Input
                                                        id="zipCode"
                                                        type="text"
                                                        placeholder="Zip Code (optional)"
                                                        onChange={(e) => setZipCode(e.target.value)}
                                                        value={zipCode}
                                                    />
                                                </div>
                                            </div>
                                        </AccordionContent>
                                    </AccordionItem>
                                </Accordion>
                                {/* <Separator /> */}

                                {/* -- Fleet Type Info -- */}
                                <div className="flex flex-row items-center justify-between">
                                    <RadioGroup defaultValue="option-one" onValueChange={(value) => setIsManagingNewFleet(value === "option-one") } className="space-y-1">
                                        <div className="flex items-center space-x-2">
                                            <RadioGroupItem value="option-one" id="option-one" />
                                            <Label htmlFor="option-one">I want to manage a new fleet.</Label>
                                        </div>
                                        <div className="flex items-center space-x-2">
                                            <RadioGroupItem value="option-two" id="option-two" />
                                            <Label htmlFor="option-two">I want access to an existing fleet.</Label>
                                        </div>
                                    </RadioGroup>

                                    {
                                        isManagingNewFleet
                                        ?
                                        <div className="grid gap-2 w-[40%]">
                                            <Label htmlFor="fleetName">Fleet Name</Label>
                                            <Input
                                                id="fleetName"
                                                type="text"
                                                placeholder="Fleet Name (required)"
                                                onChange={(e) => setNewFleetName(e.target.value)}
                                                value={newFleetName}
                                            />
                                        </div>
                                        :
                                        <div className="grid gap-2 w-[40%]">
                                            <Label htmlFor="fleetTarget">Fleet Target</Label>
                                            <Select onValueChange={(value) => setSelectedFleetName(value)} value={selectedFleetName}>
                                                <SelectTrigger id="fleetTarget" className="w-full">
                                                    <SelectValue className="text-muted-foreground" placeholder="Select a fleet (required)" />
                                                </SelectTrigger>
                                                <SelectContent className="max-h-[300px] overflow-y-auto">
                                                    {
                                                        (fleetList.length === 0)
                                                        ?
                                                        <SelectItem disabled value="">No fleets available</SelectItem>
                                                        :
                                                        fleetList.map((fleet, index) => (
                                                            <SelectItem key={index} value={fleet}>{fleet}</SelectItem>
                                                        ))
                                                    }
                                                </SelectContent>
                                            </Select>
                                        </div>
                                    }
                                </div>

                            </div>
                        </form>
                    </CardContent>
                    <CardFooter className="flex-col gap-2">
                        {
                            isLoading
                            ?
                            <Button className="w-full" disabled>
                                <Loader2Icon className="animate-spin" />
                                Please wait...
                            </Button>
                            :
                            <Button type="submit" className="w-full" disabled={submitDisabled} onClick={submitRegister}>
                                Create Account
                            </Button>
                        }
                    </CardFooter>
                </Card>
            </motion.div>
        );
    };

    return render();

}