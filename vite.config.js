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
                // Each key here will produce a bundle named <key>-bundle.js.
                theme_preload: resolve(__dirname, 'src/main/javascript/theme_preload.js'),
                aggregate: resolve(__dirname, 'src/main/javascript/aggregate.js'),
                aggregate_trends: resolve(__dirname, 'src/main/javascript/aggregate_trends.js'),
                airsync_imports: resolve(__dirname, 'src/main/javascript/airsync_imports.js'),
                airsync_uploads: resolve(__dirname, 'src/main/javascript/airsync_uploads.js'),
                create_account: resolve(__dirname, 'src/main/javascript/create_account.js'),
                create_event: resolve(__dirname, 'src/main/javascript/create_event.js'),
                event_definitions_display: resolve(__dirname, 'src/main/javascript/event_definitions_display.js'),
                event_statistics: resolve(__dirname, 'src/main/javascript/event_statistics.js'),
                flight: resolve(__dirname, 'src/main/javascript/flight.js'),
                flight_display: resolve(__dirname, 'src/main/javascript/flight_display.js'),
                flights: resolve(__dirname, 'src/main/javascript/flights.js'),
                forgot_password: resolve(__dirname, 'src/main/javascript/forgot_password.js'),
                home_navbar: resolve(__dirname, 'src/main/javascript/home_navbar.js'),
                imports: resolve(__dirname, 'src/main/javascript/imports.js'),
                manage_events: resolve(__dirname, 'src/main/javascript/manage_events.js'),
                manage_fleet: resolve(__dirname, 'src/main/javascript/manage_fleet.js'),
                reset_password: resolve(__dirname, 'src/main/javascript/reset_password.js'),
                severities: resolve(__dirname, 'src/main/javascript/severities.js'),
                system_ids: resolve(__dirname, 'src/main/javascript/system_ids.js'),
                fleet_trends: resolve(__dirname, 'src/main/javascript/fleet_trends.js'),
                ttf: resolve(__dirname, 'src/main/javascript/ttf.js'),
                update_event: resolve(__dirname, 'src/main/javascript/update_event.js'),
                update_password: resolve(__dirname, 'src/main/javascript/update_password.js'),
                update_profile: resolve(__dirname, 'src/main/javascript/update_profile.js'),
                uploads: resolve(__dirname, 'src/main/javascript/uploads.js'),
                user_preferences: resolve(__dirname, 'src/main/javascript/user_preferences.js'),
                welcome: resolve(__dirname, 'src/main/javascript/welcome.js')
            },
            output: {
                // Name each output bundle as <key>-bundle.js
                entryFileNames: '[name]-bundle.js'
            }
        }
    }
});
