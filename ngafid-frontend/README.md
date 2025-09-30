# NGAFID Frontend

Contains the React frontend. The new version now uses Vite instead of Webpack, and no longer relies on .html file templates for each page.

---

### Basic Usage

1. Ensure that you're inside the NGAFID Frontend directory:
```
    cd ./ngafid-frontend
```

2. Install Dependencies
~~~
    npm install
~~~

3. Run the Vite development server
~~~
    npm run dev
~~~

3. Connect to the device at the given IP address.

<span style="opacity:0.50">🛈 If configured correctly, you can also connect to the Vite server from other devices on your network. (Trying to host from inside WSL may require additional setup inside Windows: https://learn.microsoft.com/en-us/windows/wsl/networking) </span>

---

### Configuration

*  Global CSS styles can be modified in ```ngafid-frontend/src/app/index.css```