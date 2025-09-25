import 'bootstrap';
import React from "react";
import { createRoot } from 'react-dom/client';

import {showErrorModal} from "./error_modal";
import SignedInNavbar from "./signed_in_navbar";

class ProfilePage extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            user: {email: ""},
            valid: {
                firstName: true,
                lastName: true,
                country: true,
                state: true,
                city: true,
                address: true,
                phone: true,
                zip: true
            }
        };

        this.emailInput = React.createRef();
        this.firstNameInput = React.createRef();
        this.lastNameInput = React.createRef();
        this.countrySelect = React.createRef();
        this.stateSelect = React.createRef();
        this.cityInput = React.createRef();
        this.addressInput = React.createRef();
        this.phoneNumberInput = React.createRef();
        this.zipCodeInput = React.createRef();
    }

    componentDidMount() {

        this.setUser(user);

    }

    setUser(user) {
        this.setState({ user: user });

        this.emailInput.current.value = user.email;

        this.firstNameInput.current.value = user.firstName;
        this.lastNameInput.current.value = user.lastName;
        this.countrySelect.current.value = user.country;
        this.stateSelect.current.value = user.state;
        this.cityInput.current.value = user.city;
        this.addressInput.current.value = user.address;
        this.phoneNumberInput.current.value = user.phoneNumber;
        this.zipCodeInput.current.value = user.zipCode;

        console.log("Setting state!");
        this.setState(user);
    }

    submitAccount(event) {
        event.preventDefault();

        let valid = true;
        for (const property in this.state.valid) {
            console.log(property);

            if (property == false) {
                valid = false;
                break;
            }
        }

        if (!valid) return;

        const submissionData = {
            email: this.emailInput.current.value,
            firstName: this.firstNameInput.current.value,
            lastName: this.lastNameInput.current.value,
            country: this.countrySelect.current.value,
            state: this.stateSelect.current.value,
            city: this.cityInput.current.value,
            address: this.addressInput.current.value,
            phoneNumber: this.phoneNumberInput.current.value,
            zipCode: this.zipCodeInput.current.value
        };


        $("#loading").show();

        $.ajax({
            type: 'PUT',
            url: '/api/user/me',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {

                console.log("Received response: ", response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    showErrorModal(response.errorTitle, response.errorMessage);
                    return false;
                }

                this.setUser(response.user);
            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Submitting Account Information", errorThrown);
            },
        });

        console.log("submitting account!");
    }

    validateFirstName() {

        const firstName = this.firstNameInput.current.value;
        this.setState({
            valid: {
                ...this.state.valid,
                firstName: (firstName.length < 64)
            }
        });

    }

    validateLastName() {

        const lastName = this.lastNameInput.current.value;
        this.setState({
            valid: {
                ...this.state.valid,
                lastName: (lastName.length < 64)
            }
        });

    }

    validateCountry() {

        const country = this.countrySelect.current.value;
        this.setState({
            valid: {
                ...this.state.valid,
                country: (country.length < 128)
            }
        });

    }

    validateState() {

        const state = this.stateSelect.current.value;
        this.setState({
            valid: {
                ...this.state.valid,
                state: (state.length < 64)
            }
        });

    }

    validateCity() {

        const city = this.cityInput.current.value;
        this.setState({
            valid: {
                ...this.state.valid,
                city: (city.length < 64)
            }
        });

    }


    validateAddress() {

        const address = this.addressInput.current.value;
        this.setState({
            valid: {
                ...this.state.valid,
                address: (address.length < 256)
            }
        });

    }

    validatePhone() {

        const phone = this.phoneNumberInput.current.value;
        console.log("Phone: '", phone, "'");

        let phoneValid;
        
        //Empty string --> Valid
        if (phone == "") {
            phoneValid = true;

        //Otherwise, check the regex
        } else {
            const re = /^(\+\d{1,2}\s)?\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4}$/;
            phoneValid = re.test(phone) && (phone.length < 24);
        }

        this.setState({
            valid: {
                ...this.state.valid,
                phone: phoneValid
            }
        });

    }

    validateZip() {

        const zip = this.zipCodeInput.current.value;
        console.log("Zip Code: '", zip, "'");

        let zipValid;

        //Empty string --> Valid
        if (zip == "") {
            zipValid = true;
        
        //Otherwise, check the regex
        } else {
            const re = /^\d{5}(?:[-\s]\d{4})?$/;
            zipValid = re.test(zip) && (zip.length < 16);
        }

        this.setState({
            valid: {
                ...this.state.valid,
                zip: zipValid
            }
        });

    }

    render() {

        const fgStyle = {opacity: 1.0};

        const formGroupStyle = {
            marginBottom: '8px'
        };

        const formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px'
        };

        const labelStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'right'
        };

        const validationMessageStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        let profileValidationMessage = "";
        let profileValidationHidden = true;

        let updateProfileDisabled = true;
        let firstNameVal = "";
        if (this.firstNameInput.current != null) firstNameVal = this.firstNameInput.current.value;
        let lastNameVal = "";
        if (this.lastNameInput.current != null) lastNameVal = this.lastNameInput.current.value;

        let countrySelectVal = "";
        if (this.countrySelect.current != null) countrySelectVal = this.countrySelect.current.value;

        let stateSelectVal = "";
        if (this.stateSelect.current != null) stateSelectVal = this.stateSelect.current.value;

        let cityInputVal = "";
        if (this.cityInput.current != null) cityInputVal = this.cityInput.current.value;

        let addressVal = "";
        if (this.addressInput.current != null) addressVal = this.addressInput.current.value;

        let zipCodeVal = "";
        if (this.zipCodeInput.current != null) zipCodeVal = this.zipCodeInput.current.value;

        let phoneNumberVal = "";
        if (this.phoneNumberInput.current != null) phoneNumberVal = this.phoneNumberInput.current.value;

        //check and see if any fields have been modified
        if (firstNameVal != this.state.user.firstName) {
            console.log(`first names different! '${  firstNameVal  }' vs '${  this.state.user.firstName  }'`);
            updateProfileDisabled = false;
        } else if (lastNameVal != this.state.user.lastName) {
            console.log(`last names different! '${  lastNameVal  }' vs '${  this.state.user.lastName  }'`);
            updateProfileDisabled = false;
        } else if (countrySelectVal != this.state.user.country) {
            console.log("countries different!");
            updateProfileDisabled = false;
        } else if (stateSelectVal != this.state.user.state) {
            console.log("states different!");
            updateProfileDisabled = false;
        } else if (cityInputVal != this.state.user.city) {
            console.log("city different!");
            updateProfileDisabled = false;
        } else if (addressVal != this.state.user.address) {
            console.log("address different!");
            updateProfileDisabled = false;
        } else if (zipCodeVal != this.state.user.zipCode) {
            console.log("zip code different!");
            updateProfileDisabled = false;
        } else if (phoneNumberVal != this.state.user.phoneNumber) {
            console.log("phone number different!");
            updateProfileDisabled = false;
        }

        //check to see if all entries are valid
        if (!this.state.valid.firstName) {
            profileValidationMessage = "First name cannot be more than 64 characters.";
            profileValidationHidden = false;

        } else if (!this.state.valid.lastName) {
            profileValidationMessage = "Last name cannot be more than 64 characters.";
            profileValidationHidden = false;

        } else if (!this.state.valid.country) {
            profileValidationMessage = "Country cannot be more than 128 characters.";
            profileValidationHidden = false;

        } else if (!this.state.valid.state) {
            profileValidationMessage = "State cannot be more than 64 characters.";
            profileValidationHidden = false;

        } else if (!this.state.valid.city) {
            profileValidationMessage = "City cannot be more than 64 characters.";
            profileValidationHidden = false;

        } else if (!this.state.valid.address) {
            profileValidationMessage = "Address cannot be more than 256 characters.";
            profileValidationHidden = false;

        } else if (!this.state.valid.phone) {
            profileValidationMessage = "Phone number was not a valid phone number. Must include area code, with optional country code.";
            profileValidationHidden = false;

        } else if (!this.state.valid.zip) {
            profileValidationMessage = "Zip code was not valid, please use either a #####, #####-#### or ##### #### format.";
            profileValidationHidden = false;
        }

        console.log(`updateProfileDisabled: ${  updateProfileDisabled  } || ${  !profileValidationHidden}`);

        updateProfileDisabled = updateProfileDisabled || !profileValidationHidden;

        console.log(`rendering with profile validation message: '${  profileValidationMessage  }' and profile validation visible: ${  profileValidationHidden}`);

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage="account" waitingUserCount={waitingUserCount} fleetManager={fleetManager}
                                    unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess}
                                    plotMapHidden={plotMapHidden}/>
                </div>


                <div style={{overflowY: "auto", flex: "1 1 auto"}}>
                    <div className="card mb-1 m-2">
                        <h5 className="card-header" style={fgStyle}>
                            Update Profile
                        </h5>

                        <div className="card-body" style={fgStyle}>

                            <form onSubmit={(event) => this.submitAccount(event)}>

                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createEmail" style={labelStyle}>Email Address</label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input type="email" ref={this.emailInput} className="form-control"
                                                   id="createEmail" aria-describedby="emailHelp" readOnly
                                                   value={this.state.user.email}/>
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createFirstName" style={labelStyle}>First Name</label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input type="text" ref={this.firstNameInput} className="form-control"
                                                   id="createFirstName" aria-describedby="firstNameHelp"
                                                   placeholder="Enter first name"
                                                   onChange={() => this.validateFirstName()}/>
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createLastName" style={labelStyle}>Last Name</label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input type="text" ref={this.lastNameInput} className="form-control"
                                                   id="createLastName" aria-describedby="lastNameHelp"
                                                   placeholder="Enter last name"
                                                   onChange={() => this.validateLastName()}/>
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="countrySelect" style={labelStyle}>Country</label>
                                        </div>
                                        <div className="p-2 flex-fill">

                                            <select id="countrySelect" ref={this.countrySelect} className="form-control"
                                                    onChange={() => this.validateCountry()}>
                                                <option value="NONE"></option>

                                                <option value="AFG">Afghanistan</option>
                                                <option value="ALA">Åland Islands</option>
                                                <option value="ALB">Albania</option>
                                                <option value="DZA">Algeria</option>
                                                <option value="ASM">American Samoa</option>
                                                <option value="AND">Andorra</option>
                                                <option value="AGO">Angola</option>
                                                <option value="AIA">Anguilla</option>
                                                <option value="ATA">Antarctica</option>
                                                <option value="ATG">Antigua and Barbuda</option>
                                                <option value="ARG">Argentina</option>
                                                <option value="ARM">Armenia</option>
                                                <option value="ABW">Aruba</option>
                                                <option value="AUS">Australia</option>
                                                <option value="AUT">Austria</option>
                                                <option value="AZE">Azerbaijan</option>
                                                <option value="BHS">Bahamas</option>
                                                <option value="BHR">Bahrain</option>
                                                <option value="BGD">Bangladesh</option>
                                                <option value="BRB">Barbados</option>
                                                <option value="BLR">Belarus</option>
                                                <option value="BEL">Belgium</option>
                                                <option value="BLZ">Belize</option>
                                                <option value="BEN">Benin</option>
                                                <option value="BMU">Bermuda</option>
                                                <option value="BTN">Bhutan</option>
                                                <option value="BOL">Bolivia, Plurinational State of</option>
                                                <option value="BES">Bonaire, Sint Eustatius and Saba</option>
                                                <option value="BIH">Bosnia and Herzegovina</option>
                                                <option value="BWA">Botswana</option>
                                                <option value="BVT">Bouvet Island</option>
                                                <option value="BRA">Brazil</option>
                                                <option value="IOT">British Indian Ocean Territory</option>
                                                <option value="BRN">Brunei Darussalam</option>
                                                <option value="BGR">Bulgaria</option>
                                                <option value="BFA">Burkina Faso</option>
                                                <option value="BDI">Burundi</option>
                                                <option value="KHM">Cambodia</option>
                                                <option value="CMR">Cameroon</option>
                                                <option value="CAN">Canada</option>
                                                <option value="CPV">Cape Verde</option>
                                                <option value="CYM">Cayman Islands</option>
                                                <option value="CAF">Central African Republic</option>
                                                <option value="TCD">Chad</option>
                                                <option value="CHL">Chile</option>
                                                <option value="CHN">China</option>
                                                <option value="CXR">Christmas Island</option>
                                                <option value="CCK">Cocos (Keeling) Islands</option>
                                                <option value="COL">Colombia</option>
                                                <option value="COM">Comoros</option>
                                                <option value="COG">Congo</option>
                                                <option value="COD">Congo, the Democratic Republic of the</option>
                                                <option value="COK">Cook Islands</option>
                                                <option value="CRI">Costa Rica</option>
                                                <option value="CIV">Côte d&#39;Ivoire</option>
                                                <option value="HRV">Croatia</option>
                                                <option value="CUB">Cuba</option>
                                                <option value="CUW">Curaçao</option>
                                                <option value="CYP">Cyprus</option>
                                                <option value="CZE">Czech Republic</option>
                                                <option value="DNK">Denmark</option>
                                                <option value="DJI">Djibouti</option>
                                                <option value="DMA">Dominica</option>
                                                <option value="DOM">Dominican Republic</option>
                                                <option value="ECU">Ecuador</option>
                                                <option value="EGY">Egypt</option>
                                                <option value="SLV">El Salvador</option>
                                                <option value="GNQ">Equatorial Guinea</option>
                                                <option value="ERI">Eritrea</option>
                                                <option value="EST">Estonia</option>
                                                <option value="ETH">Ethiopia</option>
                                                <option value="FLK">Falkland Islands (Malvinas)</option>
                                                <option value="FRO">Faroe Islands</option>
                                                <option value="FJI">Fiji</option>
                                                <option value="FIN">Finland</option>
                                                <option value="FRA">France</option>
                                                <option value="GUF">French Guiana</option>
                                                <option value="PYF">French Polynesia</option>
                                                <option value="ATF">French Southern Territories</option>
                                                <option value="GAB">Gabon</option>
                                                <option value="GMB">Gambia</option>
                                                <option value="GEO">Georgia</option>
                                                <option value="DEU">Germany</option>
                                                <option value="GHA">Ghana</option>
                                                <option value="GIB">Gibraltar</option>
                                                <option value="GRC">Greece</option>
                                                <option value="GRL">Greenland</option>
                                                <option value="GRD">Grenada</option>
                                                <option value="GLP">Guadeloupe</option>
                                                <option value="GUM">Guam</option>
                                                <option value="GTM">Guatemala</option>
                                                <option value="GGY">Guernsey</option>
                                                <option value="GIN">Guinea</option>
                                                <option value="GNB">Guinea-Bissau</option>
                                                <option value="GUY">Guyana</option>
                                                <option value="HTI">Haiti</option>
                                                <option value="HMD">Heard Island and McDonald Islands</option>
                                                <option value="VAT">Holy See (Vatican City State)</option>
                                                <option value="HND">Honduras</option>
                                                <option value="HKG">Hong Kong</option>
                                                <option value="HUN">Hungary</option>
                                                <option value="ISL">Iceland</option>
                                                <option value="IND">India</option>
                                                <option value="IDN">Indonesia</option>
                                                <option value="IRN">Iran, Islamic Republic of</option>
                                                <option value="IRQ">Iraq</option>
                                                <option value="IRL">Ireland</option>
                                                <option value="IMN">Isle of Man</option>
                                                <option value="ISR">Israel</option>
                                                <option value="ITA">Italy</option>
                                                <option value="JAM">Jamaica</option>
                                                <option value="JPN">Japan</option>
                                                <option value="JEY">Jersey</option>
                                                <option value="JOR">Jordan</option>
                                                <option value="KAZ">Kazakhstan</option>
                                                <option value="KEN">Kenya</option>
                                                <option value="KIR">Kiribati</option>
                                                <option value="PRK">Korea, Democratic People&#39;s Republic of</option>
                                                <option value="KOR">Korea, Republic of</option>
                                                <option value="KWT">Kuwait</option>
                                                <option value="KGZ">Kyrgyzstan</option>
                                                <option value="LAO">Lao People&#39;s Democratic Republic</option>
                                                <option value="LVA">Latvia</option>
                                                <option value="LBN">Lebanon</option>
                                                <option value="LSO">Lesotho</option>
                                                <option value="LBR">Liberia</option>
                                                <option value="LBY">Libya</option>
                                                <option value="LIE">Liechtenstein</option>
                                                <option value="LTU">Lithuania</option>
                                                <option value="LUX">Luxembourg</option>
                                                <option value="MAC">Macao</option>
                                                <option value="MKD">Macedonia, the former Yugoslav Republic of</option>
                                                <option value="MDG">Madagascar</option>
                                                <option value="MWI">Malawi</option>
                                                <option value="MYS">Malaysia</option>
                                                <option value="MDV">Maldives</option>
                                                <option value="MLI">Mali</option>
                                                <option value="MLT">Malta</option>
                                                <option value="MHL">Marshall Islands</option>
                                                <option value="MTQ">Martinique</option>
                                                <option value="MRT">Mauritania</option>
                                                <option value="MUS">Mauritius</option>
                                                <option value="MYT">Mayotte</option>
                                                <option value="MEX">Mexico</option>
                                                <option value="FSM">Micronesia, Federated States of</option>
                                                <option value="MDA">Moldova, Republic of</option>
                                                <option value="MCO">Monaco</option>
                                                <option value="MNG">Mongolia</option>
                                                <option value="MNE">Montenegro</option>
                                                <option value="MSR">Montserrat</option>
                                                <option value="MAR">Morocco</option>
                                                <option value="MOZ">Mozambique</option>
                                                <option value="MMR">Myanmar</option>
                                                <option value="NAM">Namibia</option>
                                                <option value="NRU">Nauru</option>
                                                <option value="NPL">Nepal</option>
                                                <option value="NLD">Netherlands</option>
                                                <option value="NCL">New Caledonia</option>
                                                <option value="NZL">New Zealand</option>
                                                <option value="NIC">Nicaragua</option>
                                                <option value="NER">Niger</option>
                                                <option value="NGA">Nigeria</option>
                                                <option value="NIU">Niue</option>
                                                <option value="NFK">Norfolk Island</option>
                                                <option value="MNP">Northern Mariana Islands</option>
                                                <option value="NOR">Norway</option>
                                                <option value="OMN">Oman</option>
                                                <option value="PAK">Pakistan</option>
                                                <option value="PLW">Palau</option>
                                                <option value="PSE">Palestinian Territory, Occupied</option>
                                                <option value="PAN">Panama</option>
                                                <option value="PNG">Papua New Guinea</option>
                                                <option value="PRY">Paraguay</option>
                                                <option value="PER">Peru</option>
                                                <option value="PHL">Philippines</option>
                                                <option value="PCN">Pitcairn</option>
                                                <option value="POL">Poland</option>
                                                <option value="PRT">Portugal</option>
                                                <option value="PRI">Puerto Rico</option>
                                                <option value="QAT">Qatar</option>
                                                <option value="REU">Réunion</option>
                                                <option value="ROU">Romania</option>
                                                <option value="RUS">Russian Federation</option>
                                                <option value="RWA">Rwanda</option>
                                                <option value="BLM">Saint Barthélemy</option>
                                                <option value="SHN">Saint Helena, Ascension and Tristan da Cunha
                                                </option>
                                                <option value="KNA">Saint Kitts and Nevis</option>
                                                <option value="LCA">Saint Lucia</option>
                                                <option value="MAF">Saint Martin (French part)</option>
                                                <option value="SPM">Saint Pierre and Miquelon</option>
                                                <option value="VCT">Saint Vincent and the Grenadines</option>
                                                <option value="WSM">Samoa</option>
                                                <option value="SMR">San Marino</option>
                                                <option value="STP">Sao Tome and Principe</option>
                                                <option value="SAU">Saudi Arabia</option>
                                                <option value="SEN">Senegal</option>
                                                <option value="SRB">Serbia</option>
                                                <option value="SYC">Seychelles</option>
                                                <option value="SLE">Sierra Leone</option>
                                                <option value="SGP">Singapore</option>
                                                <option value="SXM">Sint Maarten (Dutch part)</option>
                                                <option value="SVK">Slovakia</option>
                                                <option value="SVN">Slovenia</option>
                                                <option value="SLB">Solomon Islands</option>
                                                <option value="SOM">Somalia</option>
                                                <option value="ZAF">South Africa</option>
                                                <option value="SGS">South Georgia and the South Sandwich Islands
                                                </option>
                                                <option value="SSD">South Sudan</option>
                                                <option value="ESP">Spain</option>
                                                <option value="LKA">Sri Lanka</option>
                                                <option value="SDN">Sudan</option>
                                                <option value="SUR">Suriname</option>
                                                <option value="SJM">Svalbard and Jan Mayen</option>
                                                <option value="SWZ">Swaziland</option>
                                                <option value="SWE">Sweden</option>
                                                <option value="CHE">Switzerland</option>
                                                <option value="SYR">Syrian Arab Republic</option>
                                                <option value="TWN">Taiwan, Province of China</option>
                                                <option value="TJK">Tajikistan</option>
                                                <option value="TZA">Tanzania, United Republic of</option>
                                                <option value="THA">Thailand</option>
                                                <option value="TLS">Timor-Leste</option>
                                                <option value="TGO">Togo</option>
                                                <option value="TKL">Tokelau</option>
                                                <option value="TON">Tonga</option>
                                                <option value="TTO">Trinidad and Tobago</option>
                                                <option value="TUN">Tunisia</option>
                                                <option value="TUR">Turkey</option>
                                                <option value="TKM">Turkmenistan</option>
                                                <option value="TCA">Turks and Caicos Islands</option>
                                                <option value="TUV">Tuvalu</option>
                                                <option value="UGA">Uganda</option>
                                                <option value="UKR">Ukraine</option>
                                                <option value="ARE">United Arab Emirates</option>
                                                <option value="GBR">United Kingdom</option>
                                                <option value="USA">United States</option>
                                                <option value="UMI">United States Minor Outlying Islands</option>
                                                <option value="URY">Uruguay</option>
                                                <option value="UZB">Uzbekistan</option>
                                                <option value="VUT">Vanuatu</option>
                                                <option value="VEN">Venezuela, Bolivarian Republic of</option>
                                                <option value="VNM">Viet Nam</option>
                                                <option value="VGB">Virgin Islands, British</option>
                                                <option value="VIR">Virgin Islands, U.S.</option>
                                                <option value="WLF">Wallis and Futuna</option>
                                                <option value="ESH">Western Sahara</option>
                                                <option value="YEM">Yemen</option>
                                                <option value="ZMB">Zambia</option>
                                                <option value="ZWE">Zimbabwe</option>
                                            </select>

                                        </div>
                                    </div>
                                </div>

                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="stateSelect" style={labelStyle}>State</label>
                                        </div>
                                        <div className="p-2 flex-fill">

                                            <select id="stateSelect" ref={this.stateSelect} className="form-control"
                                                    onChange={() => this.validateState()}>
                                                <option value="NONE"></option>

                                                <option value="AL">Alabama</option>
                                                <option value="AK">Alaska</option>
                                                <option value="AZ">Arizona</option>
                                                <option value="AR">Arkansas</option>
                                                <option value="CA">California</option>
                                                <option value="CO">Colorado</option>
                                                <option value="CT">Connecticut</option>
                                                <option value="DE">Delaware</option>
                                                <option value="DC">District Of Columbia</option>
                                                <option value="FL">Florida</option>
                                                <option value="GA">Georgia</option>
                                                <option value="HI">Hawaii</option>
                                                <option value="ID">Idaho</option>
                                                <option value="IL">Illinois</option>
                                                <option value="IN">Indiana</option>
                                                <option value="IA">Iowa</option>
                                                <option value="KS">Kansas</option>
                                                <option value="KY">Kentucky</option>
                                                <option value="LA">Louisiana</option>
                                                <option value="ME">Maine</option>
                                                <option value="MD">Maryland</option>
                                                <option value="MA">Massachusetts</option>
                                                <option value="MI">Michigan</option>
                                                <option value="MN">Minnesota</option>
                                                <option value="MS">Mississippi</option>
                                                <option value="MO">Missouri</option>
                                                <option value="MT">Montana</option>
                                                <option value="NE">Nebraska</option>
                                                <option value="NV">Nevada</option>
                                                <option value="NH">New Hampshire</option>
                                                <option value="NJ">New Jersey</option>
                                                <option value="NM">New Mexico</option>
                                                <option value="NY">New York</option>
                                                <option value="NC">North Carolina</option>
                                                <option value="ND">North Dakota</option>
                                                <option value="OH">Ohio</option>
                                                <option value="OK">Oklahoma</option>
                                                <option value="OR">Oregon</option>
                                                <option value="PA">Pennsylvania</option>
                                                <option value="RI">Rhode Island</option>
                                                <option value="SC">South Carolina</option>
                                                <option value="SD">South Dakota</option>
                                                <option value="TN">Tennessee</option>
                                                <option value="TX">Texas</option>
                                                <option value="UT">Utah</option>
                                                <option value="VT">Vermont</option>
                                                <option value="VA">Virginia</option>
                                                <option value="WA">Washington</option>
                                                <option value="WV">West Virginia</option>
                                                <option value="WI">Wisconsin</option>
                                                <option value="WY">Wyoming</option>

                                                <option value="AS">American Samoa</option>
                                                <option value="GU">Guam</option>
                                                <option value="MP">Northern Mariana Islands</option>
                                                <option value="PR">Puerto Rico</option>
                                                <option value="UM">United States Minor Outlying Islands</option>
                                                <option value="VI">Virgin Islands</option>
                                            </select>


                                        </div>
                                    </div>
                                </div>

                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createCity" style={labelStyle}>City</label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input type="text" ref={this.cityInput} className="form-control"
                                                   id="createCity" aria-describedby="cityHelp" placeholder="Enter city"
                                                   onChange={() => this.validateCity()}/>
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createAddress" style={labelStyle}>Address</label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input type="text" ref={this.addressInput} className="form-control"
                                                   id="createAddress" aria-describedby="addressHelp"
                                                   placeholder="Enter address" onChange={() => this.validateAddress()}/>
                                        </div>
                                    </div>
                                </div>

                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createPhoneNumber" style={labelStyle}>Phone Number</label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input type="text" ref={this.phoneNumberInput} className="form-control"
                                                   id="createPhoneNumber" aria-describedby="phoneNumberHelp"
                                                   placeholder="Enter phone number"
                                                   onChange={() => this.validatePhone()}/>
                                        </div>
                                    </div>
                                </div>


                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createZipCode" style={labelStyle}>Zip Code</label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input type="text" ref={this.zipCodeInput} className="form-control"
                                                   id="createZipCode" aria-describedby="zipCodeHelp"
                                                   placeholder="Enter zip code" onChange={() => this.validateZip()}/>
                                        </div>
                                    </div>
                                </div>

                                <div className="d-flex">
                                    <div className="p-2" style={formHeaderStyle}>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <span style={validationMessageStyle}
                                              hidden={profileValidationHidden}>{profileValidationMessage}</span>
                                    </div>
                                    <div className="p-2">
                                        <button type="submit" className="btn btn-primary float-right"
                                                disabled={updateProfileDisabled}>Update Profile
                                        </button>
                                    </div>
                                </div>

                            </form>

                        </div>
                    </div>
                </div>

            </div>
        );
    }
}

const container = document.querySelector("#profile-page");
const root = createRoot(container);
root.render(<ProfilePage/>);