// src/app/components/navbars/navbar_slot.tsx
import React from "react";

type Context = {
    extras: React.ReactNode[];
    setExtras: (n: React.ReactNode[]) => void;
};

const NavbarSlotContext = React.createContext<Context | null>(null);

export function NavbarSlotProvider({ children }: { children: React.ReactNode }) {

    const [extras, setExtras] = React.useState<React.ReactNode[]>([]);
    const value = React.useMemo(() => ({ extras, setExtras }), [extras]);

    return <NavbarSlotContext.Provider value={value}>
        {children}
    </NavbarSlotContext.Provider>;

}

export function useNavbarSlot() {

    const context = React.useContext(NavbarSlotContext);
    if (!context)
        throw new Error("useNavbarSlot must be used within NavbarSlotProvider");

    return context;

}

function isFragmentElement(el: React.ReactNode): el is React.ReactElement<{ children?: React.ReactNode }> {
    return React.isValidElement(el) && (el.type === React.Fragment);
}

// Recursively flatten Fragments
function flattenChildren(node: React.ReactNode): React.ReactNode[] {

    return React.Children.toArray(node).flatMap((child) => {

        // Child is a Fragment, recurse
        if (isFragmentElement(child))
            return flattenChildren(child.props.children);
        
        return [child];

    });

}

export function NavbarExtras({ children }: { children: React.ReactNode }) {

    const { setExtras } = useNavbarSlot();

    React.useEffect(() => {

        const flat = flattenChildren(children).filter(Boolean);
        setExtras(flat);
        return () => setExtras([]);
        
    }, [children, setExtras]);

    return null;
}
