# NGAFID Frontend

Contains the React frontend. The new version now uses Vite instead of Webpack, and no longer relies on .html file templates for each page.

---

### Basic Usage

1. Ensure that you're inside the NGAFID Frontend directory:
```
    cd ./ngafid-frontend
```

2. Install Dependencies:
~~~
    npm install
~~~

3. Run the Vite development server:
~~~
    npm run dev
~~~

3. Connect to the device at the given IP address.

<span style="opacity:0.50">🛈 If configured correctly, you can also connect to the Vite server from other devices on your network. (Trying to host from inside WSL may require additional setup inside Windows: https://learn.microsoft.com/en-us/windows/wsl/networking) </span>

---

### Configuration

*  Global CSS styles can be modified in ```ngafid-frontend/src/app/index.css```
    * Styling pertaining to just a single page should be added in separate .css files adjacent to the page's .tsx file.

---

### Providers

Use **Providers** to pass globally-accessible values down the component tree.

1. For example, the ```ThemeProvider``` lets its ```theme``` state setter be accessed with ```{setTheme} = useTheme();```, as long as ```useTheme``` is imported inside the component(s) using it.

1. After creating a Provider, add it to the ```providerTree``` array inside ```main.tsx```.

3. The order that the Providers are defined in the array matters.

---

### Pages & Routes

Page Routes will be automatically generated inside ```main.tsx``` without having to define them manually.

1. Pages live inside the ```ngafid-frontend/src/components/pages``` directory.
2. Protected pages (requiring the user to be logged in) live inside the ```/protected``` sub-directory (pages outside of this subdirectory can be considered public, and won't require a login to access).
3. To add a new page, create a new folder with the page's name, and add a new .tsx file with the page's name to that folder.
4. Folders with a leading underscore (e.g., ```.../pages/summary/_charts/...```) will be excluded from the Route generation. (See ```isPageCandidate``` in ```main.tsx``` for additional rules.)