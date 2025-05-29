import React from 'react';
import { createRoot } from 'react-dom/client';
import SignedInNavbar from './signed_in_navbar';
import { errorModal } from './error_modal';

class ProximityMapPage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            events: [],
            loading: false,
            error: null
        };
    }

    componentDidMount() {
        this.loadEvents();
    }

    loadEvents = () => {
        this.setState({ loading: true });
        $.ajax({
            type: 'GET',
            url: '/protected/all_proximity_events',
            dataType: 'json',
            success: (response) => {
                this.setState({ events: response, loading: false });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                const errorMessage = jqXHR.responseText || errorThrown;
                this.setState({ 
                    error: errorMessage,
                    loading: false 
                });
                errorModal.show("Error Loading Proximity Events", errorMessage);
            }
        });
    }

    formatDateTime = (timestamp) => {
        if (!timestamp) return '';
        const date = new Date(timestamp);
        return date.toLocaleString();
    }

    renderEventList = () => {
        const { events } = this.state;
        
        if (events.length === 0) {
            return <div className="alert alert-info">No proximity events found.</div>;
        }

        return (
            <div className="table-responsive">
                <table className="table table-striped table-hover">
                    <thead>
                        <tr>
                            <th>Flight ID</th>
                            <th>Other Flight ID</th>
                            <th>Start Line</th>
                            <th>End Line</th>
                            <th>Start Time</th>
                            <th>End Time</th>
                            <th>Severity</th>
                            <th>Lateral Distance (ft)</th>
                            <th>Vertical Distance (ft)</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {events.map(event => (
                            <tr key={event.id}>
                                <td>{event.flightId}</td>
                                <td>{event.otherFlightId}</td>
                                <td>{event.startLine}</td>
                                <td>{event.endLine}</td>
                                <td>{this.formatDateTime(event.startTime)}</td>
                                <td>{this.formatDateTime(event.endTime)}</td>
                                <td>
                                    <span className={`badge badge-${event.severity > 0.7 ? 'danger' : event.severity > 0.3 ? 'warning' : 'info'}`}>
                                        {event.severity.toFixed(2)}
                                    </span>
                                </td>
                                <td>{event.lateralDistance ? event.lateralDistance.toFixed(2) : 'N/A'}</td>
                                <td>{event.verticalDistance ? event.verticalDistance.toFixed(2) : 'N/A'}</td>
                                <td>
                                    <button 
                                        className="btn btn-sm btn-primary"
                                        onClick={() => this.showEventDetails(event.id)}
                                    >
                                        Details
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        );
    }

    showEventDetails = (eventId) => {
        $.ajax({
            type: 'GET',
            url: `/protected/proximity_event_details/${eventId}`,
            dataType: 'json',
            success: (response) => {
                // TODO: Show event details in a modal or expand the row
                console.log('Event details:', response);
            },
            error: (jqXHR, textStatus, errorThrown) => {
                const errorMessage = jqXHR.responseText || errorThrown;
                errorModal.show("Error Loading Event Details", errorMessage);
            }
        });
    }

    render() {
        const { loading, error } = this.state;

        return (
            <div className="container-fluid mt-4">
                <div className="row">
                    <div className="col-12">
                        <h1>Proximity Events</h1>
                        {error && (
                            <div className="alert alert-danger">
                                Error loading events: {error}
                            </div>
                        )}
                        {loading ? (
                            <div className="text-center">
                                <div className="spinner-border" role="status">
                                    <span className="sr-only">Loading...</span>
                                </div>
                            </div>
                        ) : (
                            this.renderEventList()
                        )}
                    </div>
                </div>
            </div>
        );
    }
}

// Wait for DOM to be ready
document.addEventListener('DOMContentLoaded', () => {
    // Render the navbar
    const navbarContainer = document.querySelector('#navbar');
    if (navbarContainer) {
        const navbarRoot = createRoot(navbarContainer);
        navbarRoot.render(<SignedInNavbar activePage="proximity_map"/>);
    }

    // Render the main page
    const pageContainer = document.querySelector('#proximity-map-page');
    if (pageContainer) {
        const pageRoot = createRoot(pageContainer);
        pageRoot.render(<ProximityMapPage/>);
    }
}); 