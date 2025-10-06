// ngafid-frontend/src/app/components/pings/ping.tsx

export enum PingColor {
    SKY = "bg-sky-500",

    RED = "bg-red-500",
    ORANGE = "bg-orange-500",
    AMBER = "bg-amber-500",
    YELLOW = "bg-yellow-500",

    GREEN = "bg-green-500",
    TEAL = "bg-teal-500",
    
    NEUTRAL = "bg-gray-200"
};

export default function Ping({color=PingColor.SKY}: {color?: PingColor}) {

    return (

        <div className="absolute flex size-3 top-0 right-0 -mr-1.5 -mt-1.5">
            <span className={`absolute inline-flex h-full w-full animate-ping rounded-full ${color} opacity-75`}></span>
            <span className={`relative inline-flex size-3 rounded-full shadow-sm ${color}`}></span>
            <span className={`absolute inline-flex size-3 rounded-full outline-[#BBBBBB]/50 outline-1`}></span>
        </div>

    );

}