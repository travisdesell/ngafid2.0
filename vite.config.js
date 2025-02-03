import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
    plugins: [react()],
    // Configure the build so that the bundles are output where your server expects them.
    build: {
        // Output directory for production bundles
        outDir: 'src/main/resources/public/js',
        rollupOptions: {
            // Define one entry per bundle.
            input: {
                // Each key here will produce a bundle named <key>-bundle.jsx.
                theme_preload: resolve(__dirname, 'src/main/javascript/theme_preload.jsx'),
                aggregate: resolve(__dirname, 'src/main/javascript/aggregate.jsx'),
                aggregate_trends: resolve(__dirname, 'src/main/javascript/aggregate_trends.jsx'),
                airsync_imports: resolve(__dirname, 'src/main/javascript/airsync_imports.jsx'),
                airsync_uploads: resolve(__dirname, 'src/main/javascript/airsync_uploads.jsx'),
                create_account: resolve(__dirname, 'src/main/javascript/create_account.jsx'),
                create_event: resolve(__dirname, 'src/main/javascript/create_event.jsx'),
                event_definitions_display: resolve(__dirname, 'src/main/javascript/event_definitions_display.jsx'),
                event_statistics: resolve(__dirname, 'src/main/javascript/event_statistics.jsx'),
                flight: resolve(__dirname, 'src/main/javascript/flight.jsx'),
                flight_display: resolve(__dirname, 'src/main/javascript/flight_display.jsx'),
                flights: resolve(__dirname, 'src/main/javascript/flights.jsx'),
                forgot_password: resolve(__dirname, 'src/main/javascript/forgot_password.jsx'),
                home_navbar: resolve(__dirname, 'src/main/javascript/home_navbar.jsx'),
                imports: resolve(__dirname, 'src/main/javascript/imports.jsx'),
                manage_events: resolve(__dirname, 'src/main/javascript/manage_events.jsx'),
                manage_fleet: resolve(__dirname, 'src/main/javascript/manage_fleet.jsx'),
                reset_password: resolve(__dirname, 'src/main/javascript/reset_password.jsx'),
                severities: resolve(__dirname, 'src/main/javascript/severities.jsx'),
                system_ids: resolve(__dirname, 'src/main/javascript/system_ids.jsx'),
                fleet_trends: resolve(__dirname, 'src/main/javascript/fleet_trends.jsx'),
                ttf: resolve(__dirname, 'src/main/javascript/ttf.jsx'),
                update_event: resolve(__dirname, 'src/main/javascript/update_event.jsx'),
                update_password: resolve(__dirname, 'src/main/javascript/update_password.jsx'),
                update_profile: resolve(__dirname, 'src/main/javascript/update_profile.jsx'),
                uploads: resolve(__dirname, 'src/main/javascript/uploads.jsx'),
                welcome: resolve(__dirname, 'src/main/javascript/welcome.jsx')
            },
            output: {
                // Name each output bundle as <key>-bundle.jsx
                entryFileNames: '[name]-bundle.jsx'
            }
        }
    }
});
