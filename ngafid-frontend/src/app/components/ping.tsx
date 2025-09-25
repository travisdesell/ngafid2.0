export default function Ping() {

    return (

        <div className="absolute flex size-3 top-0 right-0 -mr-1.5 -mt-1.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-sky-400 opacity-75"></span>
            <span className="relative inline-flex size-3 rounded-full bg-sky-500"></span>
        </div>

    );

}