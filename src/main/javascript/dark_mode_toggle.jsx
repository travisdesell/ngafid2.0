import React, { Component } from 'react';

export class DarkModeToggle extends Component {

    constructor(props) {

        super(props);

        //Initialize state based on localStorage value
        const darkMode = localStorage.getItem('darkMode') === 'true';
        this.state = {
            useDarkMode: darkMode,
        };

        this.updateDarkMode = this.updateDarkMode.bind(this);

    }

    componentDidMount() {

        const darkMode = localStorage.getItem('darkMode') === 'true';
        this.setState(
            { useDarkMode: darkMode }, () => {this.applyTheme(this.state.useDarkMode);}
        );

    }


    applyTheme(useDarkMode) {

        const root = document.documentElement;
        if (useDarkMode)
            root.classList.add('dark-theme');
        else
            root.classList.remove('dark-theme');

    }

    updateDarkMode() {

        const newDarkMode = !this.state.useDarkMode;

        //Update the localStorage value
        localStorage.setItem('darkMode', newDarkMode ? 'true' : 'false');

        //Apply the theme
        this.applyTheme(newDarkMode);

        //Update the state
        this.setState({ useDarkMode: newDarkMode });

        if (this.props.onClickAlt)
            this.props.onClickAlt();

    }

    render() {
        return (
            <a>
                <div className="button-edge">
                    <button className="button-behind" onClick={this.updateDarkMode}>
                        {this.state.useDarkMode ? (
                            <div className="button fa fa-sun-o" />
                        ) : (
                            <div className="button fa fa-moon-o" />
                        )}
                    </button>
                </div>
            </a>
        );
    }
}
