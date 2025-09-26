// ngafid-frontend/src/app/pages/status/status.tsx
import ProtectedNavbar from "@/components/navbars/protected_navbar";

export default function Status() {

    const render = () => {

        return (
            <div className="overflow-x-hidden flex flex-col h-[100vh]">

                {/* Navbar */}
                <ProtectedNavbar />

                {/* Page Content */}
                <div className="flex flex-col p-4 flex-1 min-h-0 overflow-y-auto">
                    <h1 className="text-2xl font-bold mb-4">Status Page</h1>
                </div>
                
            </div>
        );

    };

    return render();

}