// ngafid-frontend/src/app/pages/protected/profile_preferences/_profile_preferences_profile_information_content.tsx
import ErrorModal from "@/components/modals/error_modal";
import { useModal } from "@/components/modals/modal_context";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { fetchJson } from "@/fetchJson";
import { type FormEvent, useEffect, useMemo, useState } from "react";

interface ProfileFormState {
    email: string;
    firstName: string;
    lastName: string;
    country: string;
    state: string;
    city: string;
    address: string;
    phoneNumber: string;
    zipCode: string;
}

interface ErrorResponse {
    errorTitle?: string;
    errorMessage?: string;
}

type ProfileResponse = Partial<ProfileFormState> & {
    user?: Partial<ProfileFormState>;
};

const SELECT_NONE_VALUE = "NONE";

const COUNTRY_OPTIONS = [
    { code: "AFG", name: "Afghanistan" },
    { code: "ALA", name: "Aland Islands" },
    { code: "ALB", name: "Albania" },
    { code: "DZA", name: "Algeria" },
    { code: "ASM", name: "American Samoa" },
    { code: "AND", name: "Andorra" },
    { code: "AGO", name: "Angola" },
    { code: "AIA", name: "Anguilla" },
    { code: "ATA", name: "Antarctica" },
    { code: "ATG", name: "Antigua and Barbuda" },
    { code: "ARG", name: "Argentina" },
    { code: "ARM", name: "Armenia" },
    { code: "ABW", name: "Aruba" },
    { code: "AUS", name: "Australia" },
    { code: "AUT", name: "Austria" },
    { code: "AZE", name: "Azerbaijan" },
    { code: "BHS", name: "Bahamas" },
    { code: "BHR", name: "Bahrain" },
    { code: "BGD", name: "Bangladesh" },
    { code: "BRB", name: "Barbados" },
    { code: "BLR", name: "Belarus" },
    { code: "BEL", name: "Belgium" },
    { code: "BLZ", name: "Belize" },
    { code: "BEN", name: "Benin" },
    { code: "BMU", name: "Bermuda" },
    { code: "BTN", name: "Bhutan" },
    { code: "BOL", name: "Bolivia, Plurinational State of" },
    { code: "BES", name: "Bonaire, Sint Eustatius and Saba" },
    { code: "BIH", name: "Bosnia and Herzegovina" },
    { code: "BWA", name: "Botswana" },
    { code: "BVT", name: "Bouvet Island" },
    { code: "BRA", name: "Brazil" },
    { code: "IOT", name: "British Indian Ocean Territory" },
    { code: "BRN", name: "Brunei Darussalam" },
    { code: "BGR", name: "Bulgaria" },
    { code: "BFA", name: "Burkina Faso" },
    { code: "BDI", name: "Burundi" },
    { code: "KHM", name: "Cambodia" },
    { code: "CMR", name: "Cameroon" },
    { code: "CAN", name: "Canada" },
    { code: "CPV", name: "Cape Verde" },
    { code: "CYM", name: "Cayman Islands" },
    { code: "CAF", name: "Central African Republic" },
    { code: "TCD", name: "Chad" },
    { code: "CHL", name: "Chile" },
    { code: "CHN", name: "China" },
    { code: "CXR", name: "Christmas Island" },
    { code: "CCK", name: "Cocos (Keeling) Islands" },
    { code: "COL", name: "Colombia" },
    { code: "COM", name: "Comoros" },
    { code: "COG", name: "Congo" },
    { code: "COD", name: "Congo, the Democratic Republic of the" },
    { code: "COK", name: "Cook Islands" },
    { code: "CRI", name: "Costa Rica" },
    { code: "CIV", name: "Cote d'Ivoire" },
    { code: "HRV", name: "Croatia" },
    { code: "CUB", name: "Cuba" },
    { code: "CUW", name: "Curacao" },
    { code: "CYP", name: "Cyprus" },
    { code: "CZE", name: "Czech Republic" },
    { code: "DNK", name: "Denmark" },
    { code: "DJI", name: "Djibouti" },
    { code: "DMA", name: "Dominica" },
    { code: "DOM", name: "Dominican Republic" },
    { code: "ECU", name: "Ecuador" },
    { code: "EGY", name: "Egypt" },
    { code: "SLV", name: "El Salvador" },
    { code: "GNQ", name: "Equatorial Guinea" },
    { code: "ERI", name: "Eritrea" },
    { code: "EST", name: "Estonia" },
    { code: "ETH", name: "Ethiopia" },
    { code: "FLK", name: "Falkland Islands (Malvinas)" },
    { code: "FRO", name: "Faroe Islands" },
    { code: "FJI", name: "Fiji" },
    { code: "FIN", name: "Finland" },
    { code: "FRA", name: "France" },
    { code: "GUF", name: "French Guiana" },
    { code: "PYF", name: "French Polynesia" },
    { code: "ATF", name: "French Southern Territories" },
    { code: "GAB", name: "Gabon" },
    { code: "GMB", name: "Gambia" },
    { code: "GEO", name: "Georgia" },
    { code: "DEU", name: "Germany" },
    { code: "GHA", name: "Ghana" },
    { code: "GIB", name: "Gibraltar" },
    { code: "GRC", name: "Greece" },
    { code: "GRL", name: "Greenland" },
    { code: "GRD", name: "Grenada" },
    { code: "GLP", name: "Guadeloupe" },
    { code: "GUM", name: "Guam" },
    { code: "GTM", name: "Guatemala" },
    { code: "GGY", name: "Guernsey" },
    { code: "GIN", name: "Guinea" },
    { code: "GNB", name: "Guinea-Bissau" },
    { code: "GUY", name: "Guyana" },
    { code: "HTI", name: "Haiti" },
    { code: "HMD", name: "Heard Island and McDonald Islands" },
    { code: "VAT", name: "Holy See (Vatican City State)" },
    { code: "HND", name: "Honduras" },
    { code: "HKG", name: "Hong Kong" },
    { code: "HUN", name: "Hungary" },
    { code: "ISL", name: "Iceland" },
    { code: "IND", name: "India" },
    { code: "IDN", name: "Indonesia" },
    { code: "IRN", name: "Iran, Islamic Republic of" },
    { code: "IRQ", name: "Iraq" },
    { code: "IRL", name: "Ireland" },
    { code: "IMN", name: "Isle of Man" },
    { code: "ISR", name: "Israel" },
    { code: "ITA", name: "Italy" },
    { code: "JAM", name: "Jamaica" },
    { code: "JPN", name: "Japan" },
    { code: "JEY", name: "Jersey" },
    { code: "JOR", name: "Jordan" },
    { code: "KAZ", name: "Kazakhstan" },
    { code: "KEN", name: "Kenya" },
    { code: "KIR", name: "Kiribati" },
    { code: "PRK", name: "Korea, Democratic People's Republic of" },
    { code: "KOR", name: "Korea, Republic of" },
    { code: "KWT", name: "Kuwait" },
    { code: "KGZ", name: "Kyrgyzstan" },
    { code: "LAO", name: "Lao People's Democratic Republic" },
    { code: "LVA", name: "Latvia" },
    { code: "LBN", name: "Lebanon" },
    { code: "LSO", name: "Lesotho" },
    { code: "LBR", name: "Liberia" },
    { code: "LBY", name: "Libya" },
    { code: "LIE", name: "Liechtenstein" },
    { code: "LTU", name: "Lithuania" },
    { code: "LUX", name: "Luxembourg" },
    { code: "MAC", name: "Macao" },
    { code: "MKD", name: "Macedonia, the former Yugoslav Republic of" },
    { code: "MDG", name: "Madagascar" },
    { code: "MWI", name: "Malawi" },
    { code: "MYS", name: "Malaysia" },
    { code: "MDV", name: "Maldives" },
    { code: "MLI", name: "Mali" },
    { code: "MLT", name: "Malta" },
    { code: "MHL", name: "Marshall Islands" },
    { code: "MTQ", name: "Martinique" },
    { code: "MRT", name: "Mauritania" },
    { code: "MUS", name: "Mauritius" },
    { code: "MYT", name: "Mayotte" },
    { code: "MEX", name: "Mexico" },
    { code: "FSM", name: "Micronesia, Federated States of" },
    { code: "MDA", name: "Moldova, Republic of" },
    { code: "MCO", name: "Monaco" },
    { code: "MNG", name: "Mongolia" },
    { code: "MNE", name: "Montenegro" },
    { code: "MSR", name: "Montserrat" },
    { code: "MAR", name: "Morocco" },
    { code: "MOZ", name: "Mozambique" },
    { code: "MMR", name: "Myanmar" },
    { code: "NAM", name: "Namibia" },
    { code: "NRU", name: "Nauru" },
    { code: "NPL", name: "Nepal" },
    { code: "NLD", name: "Netherlands" },
    { code: "NCL", name: "New Caledonia" },
    { code: "NZL", name: "New Zealand" },
    { code: "NIC", name: "Nicaragua" },
    { code: "NER", name: "Niger" },
    { code: "NGA", name: "Nigeria" },
    { code: "NIU", name: "Niue" },
    { code: "NFK", name: "Norfolk Island" },
    { code: "MNP", name: "Northern Mariana Islands" },
    { code: "NOR", name: "Norway" },
    { code: "OMN", name: "Oman" },
    { code: "PAK", name: "Pakistan" },
    { code: "PLW", name: "Palau" },
    { code: "PSE", name: "Palestinian Territory, Occupied" },
    { code: "PAN", name: "Panama" },
    { code: "PNG", name: "Papua New Guinea" },
    { code: "PRY", name: "Paraguay" },
    { code: "PER", name: "Peru" },
    { code: "PHL", name: "Philippines" },
    { code: "PCN", name: "Pitcairn" },
    { code: "POL", name: "Poland" },
    { code: "PRT", name: "Portugal" },
    { code: "PRI", name: "Puerto Rico" },
    { code: "QAT", name: "Qatar" },
    { code: "REU", name: "Reunion" },
    { code: "ROU", name: "Romania" },
    { code: "RUS", name: "Russian Federation" },
    { code: "RWA", name: "Rwanda" },
    { code: "BLM", name: "Saint Barthelemy" },
    { code: "SHN", name: "Saint Helena, Ascension and Tristan da Cunha" },
    { code: "KNA", name: "Saint Kitts and Nevis" },
    { code: "LCA", name: "Saint Lucia" },
    { code: "MAF", name: "Saint Martin (French part)" },
    { code: "SPM", name: "Saint Pierre and Miquelon" },
    { code: "VCT", name: "Saint Vincent and the Grenadines" },
    { code: "WSM", name: "Samoa" },
    { code: "SMR", name: "San Marino" },
    { code: "STP", name: "Sao Tome and Principe" },
    { code: "SAU", name: "Saudi Arabia" },
    { code: "SEN", name: "Senegal" },
    { code: "SRB", name: "Serbia" },
    { code: "SYC", name: "Seychelles" },
    { code: "SLE", name: "Sierra Leone" },
    { code: "SGP", name: "Singapore" },
    { code: "SXM", name: "Sint Maarten (Dutch part)" },
    { code: "SVK", name: "Slovakia" },
    { code: "SVN", name: "Slovenia" },
    { code: "SLB", name: "Solomon Islands" },
    { code: "SOM", name: "Somalia" },
    { code: "ZAF", name: "South Africa" },
    { code: "SGS", name: "South Georgia and the South Sandwich Islands" },
    { code: "SSD", name: "South Sudan" },
    { code: "ESP", name: "Spain" },
    { code: "LKA", name: "Sri Lanka" },
    { code: "SDN", name: "Sudan" },
    { code: "SUR", name: "Suriname" },
    { code: "SJM", name: "Svalbard and Jan Mayen" },
    { code: "SWZ", name: "Swaziland" },
    { code: "SWE", name: "Sweden" },
    { code: "CHE", name: "Switzerland" },
    { code: "SYR", name: "Syrian Arab Republic" },
    { code: "TWN", name: "Taiwan, Province of China" },
    { code: "TJK", name: "Tajikistan" },
    { code: "TZA", name: "Tanzania, United Republic of" },
    { code: "THA", name: "Thailand" },
    { code: "TLS", name: "Timor-Leste" },
    { code: "TGO", name: "Togo" },
    { code: "TKL", name: "Tokelau" },
    { code: "TON", name: "Tonga" },
    { code: "TTO", name: "Trinidad and Tobago" },
    { code: "TUN", name: "Tunisia" },
    { code: "TUR", name: "Turkey" },
    { code: "TKM", name: "Turkmenistan" },
    { code: "TCA", name: "Turks and Caicos Islands" },
    { code: "TUV", name: "Tuvalu" },
    { code: "UGA", name: "Uganda" },
    { code: "UKR", name: "Ukraine" },
    { code: "ARE", name: "United Arab Emirates" },
    { code: "GBR", name: "United Kingdom" },
    { code: "USA", name: "United States" },
    { code: "UMI", name: "United States Minor Outlying Islands" },
    { code: "URY", name: "Uruguay" },
    { code: "UZB", name: "Uzbekistan" },
    { code: "VUT", name: "Vanuatu" },
    { code: "VEN", name: "Venezuela, Bolivarian Republic of" },
    { code: "VNM", name: "Viet Nam" },
    { code: "VGB", name: "Virgin Islands, British" },
    { code: "VIR", name: "Virgin Islands, U.S." },
    { code: "WLF", name: "Wallis and Futuna" },
    { code: "ESH", name: "Western Sahara" },
    { code: "YEM", name: "Yemen" },
    { code: "ZMB", name: "Zambia" },
    { code: "ZWE", name: "Zimbabwe" },
];

const US_STATE_OPTIONS = [
    { code: "AL", name: "Alabama" },
    { code: "AK", name: "Alaska" },
    { code: "AZ", name: "Arizona" },
    { code: "AR", name: "Arkansas" },
    { code: "CA", name: "California" },
    { code: "CO", name: "Colorado" },
    { code: "CT", name: "Connecticut" },
    { code: "DE", name: "Delaware" },
    { code: "DC", name: "District Of Columbia" },
    { code: "FL", name: "Florida" },
    { code: "GA", name: "Georgia" },
    { code: "HI", name: "Hawaii" },
    { code: "ID", name: "Idaho" },
    { code: "IL", name: "Illinois" },
    { code: "IN", name: "Indiana" },
    { code: "IA", name: "Iowa" },
    { code: "KS", name: "Kansas" },
    { code: "KY", name: "Kentucky" },
    { code: "LA", name: "Louisiana" },
    { code: "ME", name: "Maine" },
    { code: "MD", name: "Maryland" },
    { code: "MA", name: "Massachusetts" },
    { code: "MI", name: "Michigan" },
    { code: "MN", name: "Minnesota" },
    { code: "MS", name: "Mississippi" },
    { code: "MO", name: "Missouri" },
    { code: "MT", name: "Montana" },
    { code: "NE", name: "Nebraska" },
    { code: "NV", name: "Nevada" },
    { code: "NH", name: "New Hampshire" },
    { code: "NJ", name: "New Jersey" },
    { code: "NM", name: "New Mexico" },
    { code: "NY", name: "New York" },
    { code: "NC", name: "North Carolina" },
    { code: "ND", name: "North Dakota" },
    { code: "OH", name: "Ohio" },
    { code: "OK", name: "Oklahoma" },
    { code: "OR", name: "Oregon" },
    { code: "PA", name: "Pennsylvania" },
    { code: "RI", name: "Rhode Island" },
    { code: "SC", name: "South Carolina" },
    { code: "SD", name: "South Dakota" },
    { code: "TN", name: "Tennessee" },
    { code: "TX", name: "Texas" },
    { code: "UT", name: "Utah" },
    { code: "VT", name: "Vermont" },
    { code: "VA", name: "Virginia" },
    { code: "WA", name: "Washington" },
    { code: "WV", name: "West Virginia" },
    { code: "WI", name: "Wisconsin" },
    { code: "WY", name: "Wyoming" },
    { code: "AS", name: "American Samoa" },
    { code: "GU", name: "Guam" },
    { code: "MP", name: "Northern Mariana Islands" },
    { code: "PR", name: "Puerto Rico" },
    { code: "UM", name: "United States Minor Outlying Islands" },
    { code: "VI", name: "Virgin Islands" },
];

const emptyProfile: ProfileFormState = {
    email: "",
    firstName: "",
    lastName: "",
    country: SELECT_NONE_VALUE,
    state: SELECT_NONE_VALUE,
    city: "",
    address: "",
    phoneNumber: "",
    zipCode: "",
};

const selectClassName = "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";

function normalizeSelectValue(value: string | null | undefined, options: Array<{ code: string; name: string }>) {
    const normalizedValue = value?.trim();
    if (!normalizedValue || normalizedValue === SELECT_NONE_VALUE)
        return SELECT_NONE_VALUE;

    const option = options.find(({ code, name }) => (
        code.toLowerCase() === normalizedValue.toLowerCase()
        || name.toLowerCase() === normalizedValue.toLowerCase()
    ));

    return option?.code ?? SELECT_NONE_VALUE;
}

function getSelectLabel(value: string, options: Array<{ code: string; name: string }>) {
    if (value === SELECT_NONE_VALUE)
        return "None";

    return options.find((option) => option.code === value)?.name;
}

function normalizeProfile(user: Partial<ProfileFormState> | null | undefined): ProfileFormState {
    const country = normalizeSelectValue(user?.country, COUNTRY_OPTIONS);
    const state = country === "USA"
        ? normalizeSelectValue(user?.state, US_STATE_OPTIONS)
        : SELECT_NONE_VALUE;

    return {
        email: user?.email ?? "",
        firstName: user?.firstName ?? "",
        lastName: user?.lastName ?? "",
        country,
        state,
        city: user?.city ?? "",
        address: user?.address ?? "",
        phoneNumber: user?.phoneNumber ?? "",
        zipCode: user?.zipCode ?? "",
    };
}

export default function ProfilePreferencesProfileInformationContent() {

    const { setModal } = useModal();

    const [profile, setProfile] = useState<ProfileFormState>(emptyProfile);
    const [profileBaseline, setProfileBaseline] = useState<ProfileFormState>(emptyProfile);
    const [saving, setSaving] = useState(false);
    const [successMessage, setSuccessMessage] = useState("");

    useEffect(() => {
        let cancelled = false;

        fetchJson.get<ProfileResponse>("/api/user/me")
            .then((response) => {
                if (cancelled)
                    return;

                const userProfile = response.user ?? response;
                const normalized = normalizeProfile(userProfile);
                setProfile(normalized);
                setProfileBaseline(normalized);
            })
            .catch((error: Error) => {
                if (!cancelled)
                    setModal(ErrorModal, { title: "Failed to load profile information", message: error.message });
            });

        return () => {
            cancelled = true;
        };
    }, [setModal]);

    const validation = useMemo(() => {
        const firstName = profile.firstName.trim();
        const lastName = profile.lastName.trim();
        const country = profile.country.trim();
        const state = profile.state.trim();
        const city = profile.city.trim();
        const address = profile.address.trim();
        const phone = profile.phoneNumber.trim();
        const zip = profile.zipCode.trim();

        if (firstName.length >= 64)
            return { valid: false, message: "First name cannot be more than 64 characters." };
        if (lastName.length >= 64)
            return { valid: false, message: "Last name cannot be more than 64 characters." };
        if (country.length >= 128)
            return { valid: false, message: "Country cannot be more than 128 characters." };
        if (state.length >= 64)
            return { valid: false, message: "State cannot be more than 64 characters." };
        if (city.length >= 64)
            return { valid: false, message: "City cannot be more than 64 characters." };
        if (address.length >= 256)
            return { valid: false, message: "Address cannot be more than 256 characters." };

        if (phone.length > 0) {
            const phoneRegex = /^(\+\d{1,2}\s)?\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4}$/;
            if (!phoneRegex.test(phone) || phone.length >= 24)
                return { valid: false, message: "Phone number was not valid. Include an area code and optional country code." };
        }

        if (zip.length > 0) {
            const zipRegex = /^\d{5}(?:[-\s]\d{4})?$/;
            if (!zipRegex.test(zip) || zip.length >= 16)
                return { valid: false, message: "Zip code was not valid. Use ##### or #####-####." };
        }

        return { valid: true, message: "" };
    }, [profile]);

    const hasChanges = useMemo(() => (
        Object.keys(profile).some((key) => (
            profile[key as keyof ProfileFormState] !== profileBaseline[key as keyof ProfileFormState]
        ))
    ), [profile, profileBaseline]);

    const canSubmit = hasChanges && validation.valid && !saving;

    const updateProfile = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!validation.valid || saving)
            return;

        setSaving(true);

        const payload = new URLSearchParams({
            firstName: profile.firstName,
            lastName: profile.lastName,
            country: profile.country,
            state: profile.state,
            city: profile.city,
            address: profile.address,
            phoneNumber: profile.phoneNumber,
            zipCode: profile.zipCode,
        });

        const response = await fetchJson.put("/api/user/me", payload).catch((error: Error) => {
            setModal(ErrorModal, { title: "Error Submitting Account Information", message: error.message });
            return null;
        });

        if (!response) {
            setSaving(false);
            return;
        }

        const errorResponse = response as ErrorResponse;
        if (errorResponse.errorTitle) {
            setModal(ErrorModal, { title: errorResponse.errorTitle, message: errorResponse.errorMessage ?? "" });
            setSaving(false);
            return;
        }

        const updatedUser = (response as { user?: Partial<ProfileFormState> }).user ?? response;
        const normalized = normalizeProfile(updatedUser as Partial<ProfileFormState>);
        setProfile(normalized);
        setProfileBaseline(normalized);
        setSuccessMessage("Profile updated successfully.");
        setSaving(false);

        window.setTimeout(() => setSuccessMessage(""), 4000);
    };

    return (
        <div className="flex flex-col gap-6">
            <Card className="card-glossy">
                <CardHeader>
                    <CardTitle>Profile Information</CardTitle>
                    <CardDescription>Update the contact details associated with your account.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={updateProfile} className="flex flex-col gap-4">

                        {/* Email Address */}
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">
                            <Label className="md:w-40" htmlFor="profile-email">Email Address</Label>
                            <Input id="profile-email" type="email" value={profile.email} readOnly disabled className="select-all" />
                            {/* <i>This cannot be changed.</i> */}
                        </div>
                        <Separator />

                        {/* First & Last Name */}
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">
                            <Label className="md:w-40" htmlFor="profile-first-name">First Name</Label>
                            <Input
                                id="profile-first-name"
                                value={profile.firstName}
                                onChange={(event) => setProfile({ ...profile, firstName: event.target.value })}
                                autoComplete="given-name"
                            />
                        </div>
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">
                            <Label className="md:w-40" htmlFor="profile-last-name">Last Name</Label>
                            <Input
                                id="profile-last-name"
                                value={profile.lastName}
                                onChange={(event) => setProfile({ ...profile, lastName: event.target.value })}
                                autoComplete="family-name"
                            />
                        </div>
                        <Separator />

                        {/* Location Info */}
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">

                            <Label className="md:w-40" htmlFor="profile-country">Country</Label>
                            <Select
                                value={profile.country}
                                onValueChange={(value) => setProfile({
                                    ...profile,
                                    country: value,
                                    state: value === "USA" ? profile.state : SELECT_NONE_VALUE,
                                })}
                            >
                                <SelectTrigger id="profile-country">
                                    <SelectValue placeholder="Select a country">
                                        {getSelectLabel(profile.country, COUNTRY_OPTIONS)}
                                    </SelectValue>
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value={SELECT_NONE_VALUE}>None</SelectItem>
                                {
                                    COUNTRY_OPTIONS.map((country) => (
                                        <SelectItem key={country.code} value={country.code}>
                                            {country.name}
                                        </SelectItem>
                                    ))
                                }
                                </SelectContent>
                            </Select>

                        </div>
                        {
                            (profile.country === "USA")
                            &&
                            <div className="flex flex-col gap-2 md:flex-row md:items-center">

                                <Label className="md:w-40" htmlFor="profile-state">State</Label>
                                <Select value={profile.state} onValueChange={(value) => setProfile({ ...profile, state: value })}>
                                    <SelectTrigger id="profile-state">
                                        <SelectValue placeholder="Select a state">
                                            {getSelectLabel(profile.state, US_STATE_OPTIONS)}
                                        </SelectValue>
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value={SELECT_NONE_VALUE}>None</SelectItem>
                                    {
                                        US_STATE_OPTIONS.map((state) => (
                                            <SelectItem key={state.code} value={state.code}>
                                                {state.name}
                                            </SelectItem>
                                        ))
                                    }
                                    </SelectContent>
                                </Select>
                            </div>
                        }
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">
                            <Label className="md:w-40" htmlFor="profile-city">City</Label>
                            <Input
                                id="profile-city"
                                value={profile.city}
                                onChange={(event) => setProfile({ ...profile, city: event.target.value })}
                                autoComplete="address-level2"
                            />
                        </div>
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">
                            <Label className="md:w-40" htmlFor="profile-address">Address</Label>
                            <Input
                                id="profile-address"
                                value={profile.address}
                                onChange={(event) => setProfile({ ...profile, address: event.target.value })}
                                autoComplete="street-address"
                            />
                        </div>
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">
                            <Label className="md:w-40" htmlFor="profile-zip">Zip Code</Label>
                            <Input
                                id="profile-zip"
                                value={profile.zipCode}
                                onChange={(event) => setProfile({ ...profile, zipCode: event.target.value })}
                                autoComplete="postal-code"
                            />
                        </div>
                        <Separator />

                        {/* Phone Number */}
                        <div className="flex flex-col gap-2 md:flex-row md:items-center">
                            <Label className="md:w-40" htmlFor="profile-phone">Phone Number</Label>
                            <Input
                                id="profile-phone"
                                value={profile.phoneNumber}
                                onChange={(event) => setProfile({ ...profile, phoneNumber: event.target.value })}
                                autoComplete="tel"
                            />
                        </div>

                        {(successMessage || !validation.valid) && (
                            <div className={validation.valid ? "text-sm text-green-600" : "text-sm text-red-500"}>
                                {validation.valid ? successMessage : validation.message}
                            </div>
                        )}

                        <div className="flex justify-end">
                            <Button type="submit" disabled={!canSubmit}>
                                {saving ? "Saving..." : "Update Profile"}
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}
