# NGAFID Frontend

Contains the React frontend. The new version now uses Vite instead of Webpack, and no longer relies on .html file templates for each page.

Note that some of the more component-specific documentation given below probably belongs in the documentation for the relevant components instead of here in the README, and should be moved out eventually.

As of writing this, the 2025-2026 frontend rework is still in progress. Changes and remaining issues are being tracked on GitHub here: https://github.com/travisdesell/ngafid2.0/issues/246

---

### Basic Usage

1. Ensure that you're inside the NGAFID Frontend directory:
```
    cd ./ngafid-frontend
```

2. Install Dependencies:
~~~
    pnpm install
~~~

3. Run the Vite development server (multiple options):
~~~
    npx vite [--port=<TARGET PORT HERE>]
~~~
~~~
    pnpm run dev [--port=<TARGET PORT HERE>]
~~~

3. Connect to the device at the given IP address.

<span style="opacity:0.50">
🛈 If configured correctly, you can also connect to the Vite server from other devices on your network. (Trying to host from inside WSL may require additional setup inside Windows: <a>https://learn.microsoft.com/en-us/windows/wsl/networking</a>) This can be particularly useful if we ever want to add proper responsive design for mobile devices.
</span>

---

### Building (& Previewing)

* Run either of these commands to build (the latter will continuously watch for file changes):

```
    pnpm build
```

```
    pnpm watch
```

<div style="opacity:0.50; color:sandybrown">
⚠ Because of validation step requirements, you may be required to create a build (i.e., populate ngafid-static) <i>before</i> you can properly start the server.
</div>
<div style="opacity:0.50; color:sandybrown">
<br>
⚠ If you run into issues building the site on the Beta server, nuking the existing ngafid-static folder may solve the problem.
<br>
<br>
</div>

* Run this command to preview the build:

```
    pnpm preview
```

<span style="opacity:0.50">
<br>
🛈 Some performance-intensive features (e.g., interactive charts/graphs) might be easier to work with when using the build preview, but don't avoid trying to do any optimization passes because of this (...or else ಠಠ).
<br>
<br>
</span>

---

### Shadcn

* The [Shadcn component library](https://ui.shadcn.com/docs/components) is used for pre-built UI components (e.g., buttons, dropdowns, etc.) and can be found in the `ngafid-frontend/src/app/components/ui` directory.
* These components are built on top of Radix primitives. Be sure not to accidentally import the Radix primitives instead of the components, otherwise the styling and some functionality will be missing.
* The components are all fully customizable in both appearance and functionality (i.e., after importing the components, we become responsible for the code).
* While some of the components can work immediately and without issue, many of them may require additional configuration or even extensive reworks to fit our needs. For example, the `Pagination` component has been significantly adjusted to allow for better control over page selection.

---

### Charts & Chart Interactivity

* Charts are currently implemented via `Recharts` (rather than `Plotly.js` like in the previous version).
* A `chart-interactions.tsx` file has been created to support reusable interactions (such as zooming and panning) across different charts, since Recharts doesn't natively support that kind of interactivity.
* While Recharts is an improvement over the previous verison in some areas (particularly visually), it seems to be less performant in some cases, particularly the Severities page's scatter plot. I've noted possible alternative libraries to try in the [issues](https://github.com/travisdesell/ngafid2.0/issues/246).
  * Make sure to check if the current version is actually unsatisfactory before trying to add new libraries.

---

### Legacy Code

As of writing this, a dozen or so legacy .jsx files still remain in the `ngafid-frontend/src/legacy` directory. Once the functionality has been integrated (or deprecated), remove the legacy code, and delete this section from the README.

---

### Configuration & Styling

*  Global CSS styles can be modified in ```ngafid-frontend/src/app/index.css```
    * Styling pertaining to just a single page should be added in separate .css files adjacent to the page's .tsx file (e.g., `welcome.css`)

---

### Pings

Three kinds of pings exist: `Ping`, `Ping Half (Left)`, and `Ping Half (Right)`. These are small, pulsing circles used to indicate that an element is / has become active or interactable.
- Pings will appear in the top right corner of a parent element with a `relative` position.
- The standard Ping can has different colors based on the `color` prop passed to it, with some predefined options defined in the `PingColor` enum.

---

### Notifications Panel

Currently unimplemented, but a `Notifications` button and dropdown are present in the navbar. Could be used for things like alerting users to new features, providing important announcements, upload status updates, fleet invitations, etc.

---

### Action Menu

* Referred to as the `Command Menu` in the codebase.
* The navbar has a button for opening the action menu; it can also be opened with a `Ctrl + K` keyboard shortcut.
* Supports different actions for things like opening pages, toggling theme settings, logging out, etc.
* Actions can be triggered by clicking on them, or by using their associated hotkeys (while the menu is open).
* Actions are grouped, and can be filtered by typing in the search bar at the top of the menu.
* Individual pages can have their own custom actions defined by invoking `useRegisterCommands` with an array of custom commands.
  * Command Data example:

        {
            id: "flights.copyFilterUrl",
            name: "Copy Filter URL",
            Icon: ClipboardCopy,
            hotkey: "Ctrl+Shift+C",
            command: () => copyFilterURL(filterRef.current),
            disabled: () => filterIsEmpty(filterRef.current),
        }
* Consider adding a theme setting to enable/disable this in the navbar.

---

### Theme Settings

* The navbar has a button for adjusting theme settings.
* Left clicking the button will toggle the light/dark mode.
* Right clicking the button will open up a modal which (as of writing this) contains the following options:
  * Switch to Light/Dark theme (same as left click)
  * Use High Contrast Charts (on by default)
  * Invert Background Image (off by default)
  * Blur Background Image (on by default)
  * Show Navbar Page Names (on by default)
* The theme settings use a `useLocalStorage` hook to automatically save the user's preferences to local storage, so they will persist across sessions.
* The right-click settings will not be available until the user has logged in.

---

### Providers

Use **Providers** to pass globally-accessible values down the component tree.

1. For example, the ```ThemeProvider``` lets its ```theme``` state setter be accessed with ```{setTheme} = useTheme();```, as long as ```useTheme``` is imported inside the component(s) using it.

1. After creating a Provider, add it to the ```providerTree``` array inside ```main.tsx```.

3. The order that the Providers are defined in the array matters. Try to avoid Providers that rely on other Providers; things can get tricky in some cases (e.g., requiring another Provider's data inside a Modal).

See the [React Documentation - createContext](https://react.dev/reference/react/createContext#provider) page for more details.

---

### Pages & Routes

Page Routes will be automatically generated inside ```main.tsx``` without having to define them manually.

<ol>
    <li>Pages live inside the <code>ngafid-frontend/src/components/pages</code> directory.
    <li>Protected pages (requiring the user to be logged in) live inside the <code>/protected</code> sub-directory (pages outside of this subdirectory can be considered public, and won't require a login to access).
    <li>Both protected and non-protected routes will be generated for pages inside the <code>/auto</code> sub-directory.
    <li>To add a new page, create a new folder with the page's name, and add a new .tsx file with the page's name to that folder.
    <li>Folders with a leading underscore (e.g., <code>.../pages/summary/_charts/...</code>) will be excluded from the Route generation. (See the <code>isPageCandidate</code> function in <code>main.tsx</code> for additional rules.)
</ol>

---

### Fetching Data

* `fetch`: The standard JavaScript `fetch` function can be used to make API calls to the backend.
* `fetchJson`: A convenient wrapper for `fetch` that is used to automatically parse JSON responses and handle errors. Obviously, don't use this for non-JSON responses.
  * Supports `get`, `post`, `put`, `delete`, and `patch` requests.

---

### Navbars

* Additional per-page content can be easily added to the Navbar by rendering a `NavbarExtras` component inside the page's .tsx file.
  * As an example, this is currently used on the `Flights` page to render panel toggles and a button to open the Event selection modal.
  * Avoid using this for controls that would be better-suited in sub-sections of the page (e.g., the map type dropdown on the `Flights` page and other pages with maps have been moved out of the Navbar and into the map cards themselves).

---

### Time Header

* The `TimeHeader` component is used on most pages to provide a consistent interface for selecting time ranges and applying filters. In this version, the Start / End dates are maintained across pages.
    * There are 3 different modes for the `initialApply` prop, which determines how the TimeHeader's `onApply` function is triggered when the page first loads:
        * `initialApply="manual"` (default): The Apply button is initially enabled when the page loads, but requires the user to click it themselves. *(Trends, Severities, TTF pages)*
        * `initialApply="automatic"`: The Time Header is applied automatically when the page loads, without the user having to click the Apply button (and the button is disabled after). *(Summary page)*
        * `initialApply="require-dep-change"`: The Apply button is initially disabled when the page loads, and will only be enabled after a change in dependencies (e.g., airframe selection, start/end date) is detected. After the initial apply, the button will return to its normal behavior of being enabled/disabled based on whether there are unapplied changes. *(Heat Map page)*
    * The `dependencies` prop can be used to specify an array of values that the Time Header should watch for changes on (e.g., airframe selection).
        * When a change in any of these dependencies is detected, the Time Header will re-apply itself based on the `initialApply` mode.
        * The Start & End dates are always automatically included as dependencies, so they don't need to be added to the array manually.
---

### Logging

* Uses a custom `Logger` class that wraps around the standard `console` logging functions.
* Each page, component, etc., can initialize its own logger with `const log = getLogger(<logger name>, [<text color>], [<type>])`.
  * The `color` and `type` parameters should have their order swapped (since that order would be more sensible)... but this would require someone (or just a script/regex/LLM) to fix all the current log initializations.
* Can explicitly invoke different log levels (e.g., `log.info()`, `log.error()`, etc.), or just use `log()` which defaults to the "info" level.
* Currently supports `log`, `info`, `warn`/`warning`, `error`, and `table` log types.

---

### TypeScript

All the code in the frontend is now written in TypeScript. Don't use JavaScript unless you have to.

* As noted in the [issues](https://github.com/travisdesell/ngafid2.0/issues/246), TypeScript v7 is planned to release somewhat soon; update to it when it does (and when all dependencies support it).

---

### Modals

* Implemented via 3 driver files: `modal_context.tsx`, `modal_provider.tsx`, and `modal_store.ts`.
* The `ModalContext` provides the context for the modals, and is used to create the `useModal` hook.
* Open a modal via calling `setModal(<Modal Component Name>, [<modal data>], [<on close callback>])` from the `useModal` hook.
  * Example:

        setModal(ConfirmModal,
            {
                title: "Confirm Tag Deletion",
                message: `Are you sure you want to delete the tag "${tag.name}"?`
                onConfirm: () => { log("Tag deleted!"); }
            },
            () => { log("Modal closed!"); }
        );
* When you create a new Modal component, define and export the type for the modal data it expects (extending the base `ModalData` type), and then cast the `data` variable from the `useModal` hook to that type.
    * Example:
    
            export type ModalDataConfirm = ModalData & {
                title: string;
                message: string;
                onConfirm?: () => void;
                buttonVariant?: ButtonVariant;
            };

            export default function ConfirmModal({ data }: ModalProps) {
                ...
                const { title, message, onConfirm, buttonVariant } = (data as ModalDataConfirm);
                ...
            }
    

---

### Page Titles

* Set with `setPageTitle`. Do not include `NGAFID` in the title, since it's appended automatically.
* There's an alternative way to set the title via rendering a `PageTitle` component, though the above approach will probably always be the prefereable option (dealer's choice, though).

--- 

### Animation

* Added the [Framer Motion](https://motion.dev/docs/react) library for animations.
* Currently, it's not used very widely across the site; most notably, it's used for the navbar background "arrow", and various parts of the Flights page.

---

### Multifleet Selection

Because fleets can be selected in different places (the navbar, the Profile Preferences page, and the Waiting modal), there is some reusable logic for handling this inside the `multifleet_select.tsx` file.

* Some of this logic should probably be moved outside the Multifleet Select component, since that's not really its intended purpose.
* Fleet data is currently fetched inside the `AuthProvider`. It may (or may not) make more sense to have this in a dedicated provider.
* There are some remaining [issues](https://github.com/travisdesell/ngafid2.0/issues/246) with fleet selection that need to be resolved (though it's mostly working fine).