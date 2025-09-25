// ngafid-frontend/src/app/components/background.tsx

//Import Background Images
import bgLight from '../../images/clouds.jpg';
import bgDark from '../../images/clouds_dark_compressed.jpg';
import { useTheme } from './theme-provider';

export default function Background() {

    /*
        Tailwind's 'dark:___' selector is inexplicably not working,
        so I'm just pulling the theme from the ThemeProvider and
        rendering the background image based on that.
    */

    const { theme } = useTheme();
    const isDarkMode = (theme === 'dark');

    return (
        <>
            <img src={bgLight} alt="Background" className={`scale-[1.1] fixed top-0 left-0 w-full h-full object-cover object-center pointer-events-none select-none -z-10 blur-md ${isDarkMode ? 'hidden' : 'block'}`} />
            <img src={bgDark} alt="Background" className={`scale-[1.1] fixed top-0 left-0 w-full h-full object-cover object-center pointer-events-none select-none -z-10 blur-md ${isDarkMode ? 'block' : 'hidden'}`} />
        </>
    );

}