// ngafid-frontend/src/app/components/pings/ping_half_right.tsx
import { Circle } from "lucide-react";

export default function PingHalfRight() {

    return (

        <div className="absolute flex size-3 top-0 right-0 -mr-1.5 -mt-1.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-sky-400 opacity-75"></span>
            <Circle className="absolute w-full h-full text-sky-500"/>
            <span className="relative inline-flex size-3 rounded-full bg-sky-500 scale-90" style={{ clipPath: "inset(0 0 0 50%)" }}></span>
        </div>

    );

}