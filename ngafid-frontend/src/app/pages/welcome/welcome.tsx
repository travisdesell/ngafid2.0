// ngafid-frontend/src/app/welcome.tsx

import React from 'react'
import WelcomeNavbar from '@/components/navbars/welcome_navbar';
import { Card, CardContent, CardHeader, CardDescription, CardFooter } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import * as TooltipPrimitive from '@radix-ui/react-tooltip'

import './welcome.css';
import { CircleQuestionMark, InfoIcon } from 'lucide-react';

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import RegisterModal from '@/components/modals/register_modal';
import { useModal } from '@/components/modals/modal_provider';


export default function Welcome() {


    const { setModal } = useModal();


    console.log("Rendering Welcome Component!");


    const tooltipAcronym = (acronym: string, fullText: string) => {
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="decoration-dotted underline cursor-help">{acronym}</span>
                </TooltipTrigger>

                <TooltipContent className="text-popover-foreground">
                    <p>{fullText}</p>
                    <TooltipPrimitive.Arrow className="fill-primary pb-[1px] transition-all" />
                </TooltipContent>
            </Tooltip>
        );
    };


    return (
        <div className="overflow-x-hidden flex flex-col h-[100vh]">

            {/* Navbar */}
            <WelcomeNavbar />

            {/* Page Content */}
            <div className="flex flex-col p-4 flex-1 min-h-0 overflow-y-auto">

                {/* Main Content */}
                <Card className="self-center my-auto w-full max-w-[min(80%,1920px)] h-[960px] max-h-[calc(100vh-8rem)] flex flex-col min-h-0 card-glossy">
                    
                    <CardHeader className='shrink-0 text-3xl home-card-header-text h-fit'>
                        Welcome to the National General Aviation Flight Information Database (NGAFID)
                    </CardHeader>

                    <Separator className="shrink-0" />

                    <CardContent className="flex-1 min-h-0 flex flex-row gap-12 p-12 overflow-y-auto">

                        {/* About */}
                        <div className="flex flex-1 flex-col gap-6">

                            {/* Section Header */}
                            <div className='text-2xl opacity-65 font-light flex items-center gap-2 border-b-1 border-gray w-fit pb-1'>
                                <InfoIcon/>
                                About
                            </div>

                            <p className="home-card-content-text">
                                The NGAFID is part of {tooltipAcronym("ASIAS", "Aviation Safety Information Analysis and Sharing")}, an {tooltipAcronym("FAA", "Federal Aviation Administration")} funded, joint government-industry, collaborative information sharing program to proactively analyze broad and extensive data sources towards the advancement of safety initiatives and the discovery of vulnerabilities in the {tooltipAcronym("NAS", "National Airspace System")}.
                            </p>
                            <p className="home-card-content-text">
                                The primary objective of ASIAS is to provide a national resource for use in discovering common, systemic safety problems that span multiple operators, fleets, and regions of the airspace. Safety information discovered through ASIAS activities is used across the aviation industry to drive improvements and support a variety of safety initiatives.
                            </p>
                            <p className="home-card-content-text">
                                The NGAFID was originally conceived to bring voluntary {tooltipAcronym("FDM", "Flight Data Monitoring")} capabilities to General Aviation, but has now expanded to include the broader aviation community.
                            </p>

                            <p className="home-card-content-text mt-6">
                            While sharing flight data is voluntary, there are many reasons pilots and operators should consider participating.
                            </p>


                        </div>

                        {/* FAQ */}
                        <Accordion className='flex flex-1 flex-col gap-6' type="multiple">

                            {/* Section Header */}
                            <div className='text-2xl opacity-65 font-light flex items-center gap-2 border-b-1 border-gray w-fit pb-1'>
                                <CircleQuestionMark/>
                                FAQ
                            </div>

                            {/* What is FDM */}
                            <AccordionItem value="item-1" className='border-0'>
                                <AccordionTrigger className="home-card-section-header-text">
                                    What is digital Flight Data Monitoring?
                                    <hr className='border-b-1 border-gray-500/25 flex flex-1 mx-4'/>
                                </AccordionTrigger>
                                <AccordionContent>
                                    <ul className='list-disc list-inside ml-8'>
                                        <li className="home-card-section-content-text">
                                            FDM is the recording of flight-related information. Analysis of FDM data can help pilots, instructors, or operator groups improve performance and safety.
                                        </li>
                                    </ul>
                                </AccordionContent>
                            </AccordionItem>

                            {/* Reasons to Participate */}
                            <AccordionItem value="item-2" className='border-0'>
                                <AccordionTrigger className="home-card-section-header-text">
                                    How will this project benefit the aviation community?
                                    <hr className='border-b-1 border-gray-500/25 flex flex-1 mx-4'/>
                                </AccordionTrigger>
                                <AccordionContent>
                                    <ul className='list-disc list-inside ml-8 home-card-section-content-text'>
                                        <li>
                                        You can replay your own flights and view your data to identify potential safety risks.
                                        </li>

                                        <li>
                                        Pilots in safety programs are less likely to be involved in an accident ({tooltipAcronym("GAO", "Government Accountability Office")} 13-36, pg. 13).
                                        </li>

                                        <li>
                                        Attitude data you collect will provide you enhanced feedback to improve your skills.
                                        </li>

                                        <li>
                                        Your data will improve safety for the entire aviation community.
                                        </li>

                                        <li>
                                            <strong>
                                                Your data cannot be used for any enforcement purposes. The FAA cannot see your data.
                                            </strong>
                                        </li>
                                    </ul>
                                </AccordionContent>
                            </AccordionItem>

                            {/* Benefits List */}
                            <AccordionItem value="item-3" className='border-0'>
                                <AccordionTrigger className="home-card-section-header-text">
                                    How will this project benefit the aviation community?
                                    <hr className='border-b-1 border-gray-500/25 flex flex-1 mx-4'/>
                                </AccordionTrigger>
                                <AccordionContent>
                                    <ul className='list-disc list-inside ml-8 home-card-section-content-text'>
                                        <li>
                                            By working together, the community will identify
                                            risks and safety hazards specific to the general
                                            aviation and other communities.
                                        </li>
                                        <li>
                                            The communities can develop and implement
                                            solutions to recognized problems.
                                        </li>
                                    </ul>
                                </AccordionContent>
                            </AccordionItem>

                            {/* Participation Info */}
                            <AccordionItem value="item-4" className='border-0'>
                                <AccordionTrigger className="home-card-section-header-text">
                                    How can I participate?
                                    <hr className='border-b-1 border-gray-500/25 flex flex-1 mx-4'/>
                                </AccordionTrigger>
                                <AccordionContent>
                                    <ul className='list-disc list-inside ml-8'>
                                        <li className="home-card-section-content-text">
                                            You can participate by uploading data from your on-board avionics (for example, a G1000 or data recorder).
                                        </li>
                                    </ul>
                                </AccordionContent>
                            </AccordionItem>
                        </Accordion>

                    </CardContent>

                    <Separator className="shrink-0" />

                    {/* Sign-Up Prompt */}
                    <CardFooter className="shrink-0 h-fit p-8">
                        <span className="home-card-footer-text">
                            <button className="text-yellow-500 hover:underline font-semibold cursor-pointer" onClick={() => setModal(RegisterModal)}>Click Here</button>
                            <span> to register for an NGAFID account</span>
                        </span>
                    </CardFooter>

                </Card>

            </div>
        </div>

    )
}
