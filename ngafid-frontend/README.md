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

---

### Modification

1. Use **Providers** to pass globally-accessible values down the component tree.

    1.0. For example, the ```ThemeProvider``` lets its ```theme``` state setter be accessed with ```{setTheme} = useTheme();```, as long as ```useTheme``` is imported inside the component(s) using it.

    1.1. After creating a Provider, add it to the ```providerTree``` array inside ```main.tsx```.

    1.2. The order that the Providers are defined in the array matters.