import { getLogger } from "@/components/providers/logger";
import { AlertDescription, AlertTitle } from "@/components/ui/alert";
import { AlertCircle, Info } from "lucide-react";
import { motion } from "motion/react";

const log = getLogger("PanelAlert", "black", "Component");

type PanelAlertProps = {
    title: string;
    description: string[]|string;
    isCritical?: boolean;
}

export default function PanelAlert(props: PanelAlertProps) {

    const { title, description, isCritical=false } = props;

    return <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className={`absolute left-1/2 top-1/2 translate-x-[-50%] translate-y-[-50%] w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap `}
    >
        {
            (isCritical)
            ? <AlertCircle />
            : <Info />
        }

        <div className="flex flex-col">
            <AlertTitle className="text-base">{title}</AlertTitle>
            <AlertDescription className="text-sm">
                {
                    Array.isArray(description)
                    ? description.map((desc, index) => (
                            <span key={index}>
                                {desc}
                                <br />
                            </span>
                        ))
                    : description
                }
            </AlertDescription>
        </div>
    </motion.div>

}