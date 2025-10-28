// ngafid-frontend/src/app/components/background.tsx

//Import Background Images
const bgLight = '/images/backgrounds/clouds.jpg';
const bgDark  = '/images/backgrounds/clouds_dark_compressed.jpg';
import { useTheme } from './providers/theme-provider';

export default function Background() {

    /*
        Tailwind's 'dark:___' selector is inexplicably not working,
        so I'm just pulling the theme from the ThemeProvider and
        rendering the background image based on that.
    */

    const { theme, useBackgroundImage } = useTheme();
    const isDarkMode = (theme === 'dark');

    // Not using background image, render nothing
    if (!useBackgroundImage)
        return null;

    // Otherwise, render background images
    return (
        <>
            <img src={bgLight} alt="Background" className={`scale-[1.1] fixed top-0 left-0 w-full h-full object-cover object-center pointer-events-none select-none -z-10 blur-md ${isDarkMode ? 'hidden' : 'block'}`} />
            <img src={bgDark} alt="Background" className={`scale-[1.1] fixed top-0 left-0 w-full h-full object-cover object-center pointer-events-none select-none -z-10 blur-md ${isDarkMode ? 'block' : 'hidden'}`} />
        </>
    );

}