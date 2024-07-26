import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

export { DarkModeToggle };

class DarkModeToggle extends React.Component {

    constructor(props) {
        super(props);

        console.log("[EX] Props:", props);
    
        //Initialize state based on localStorage value
        const darkMode = localStorage.getItem('darkMode') === 'true';

        this.state = {
            useDarkMode: darkMode,
            onClickAlt: (props.onClickAlt ?? (() => {}))
        };

        this.updateDarkMode = this.updateDarkMode.bind(this);
    }

    componentDidMount() {
        
        //Update the CSS variables for light/dark mode based on initial state
        const root = document.documentElement;
        if (this.state.useDarkMode)
            root.classList.add('dark-theme');
        else
            root.classList.remove('dark-theme');
    }

    updateDarkMode() {

        const newDarkMode = !this.state.useDarkMode;

        //Update the localStorage value
        localStorage.setItem('darkMode', newDarkMode ? 'true' : 'false');

        //Update the CSS variables for light/dark mode
        const root = document.documentElement;
        if (newDarkMode)
            root.classList.add('dark-theme');
        else
            root.classList.remove('dark-theme');

        // Update the state
        this.setState({ useDarkMode: newDarkMode });

        this.state.onClickAlt();
    }

    render() {
        return (
            <a>
                <div className="button-edge">
                    <button className="button-behind" onClick={this.updateDarkMode}>
                        {
                            this.state.useDarkMode
                            ? <div className="button fa fa-sun-o"/>
                            : <div className="button fa fa-moon-o"/>
                        }
                    </button>
                </div>
            </a>
        );
    }
}
