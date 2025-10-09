import React, {useState} from 'react';
import {IconButton, Input, MenuItem, Paper} from '@mui/material';
import PropTypes from 'prop-types';
import clsx from 'clsx';
import Select from '@mui/material/Select';
import {Search} from '@mui/icons-material';
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {
        borderRadius: '4px',
        alignItems: 'center',
        padding: ({theme}) => theme.spacing(1, 2),
        display: 'flex',
        flexBasis: 420
    },
    icon: {
        marginRight: ({theme}) => theme.spacing(1),
        color: ({theme}) => theme.palette.text.secondary
    },
    input: {
        flexGrow: 1,
        fontSize: '14px',
        lineHeight: '16px',
        letterSpacing: '-0.05px'
    }
});

const SearchInput = props => {
    const {className, onChange, selects, select, setSelect, style, initValue, ...rest} = props;
    const theme = useTheme();
    const classes = useStyles({theme});
    const [value, setValue] = useState(initValue || '');

    return (
        <Paper
            {...rest}
            className={clsx(classes.root, className)}
            style={style}
        >
            {
                selects && selects.length > 0 ? (
                    <Select
                        value={select || 'all'}
                        onChange={e => setSelect(e.target.value)}
                        disableUnderline
                    >
                        {selects.map((s, i) => (
                            <MenuItem key={String(i)} value={s.value}>{s.key}</MenuItem>
                        ))}
                    </Select>
                ) : null
            }
            <Input
                {...rest}
                value={value}
                // onKeyUp={e => {
                //   if (e.keystatus === 13) {
                //     onChange(value)
                //   }
                // }}
                onChange={(e) => {
                    const value = e.target.value;
                    setValue(value);
                    onChange(value);
                }}
                className={classes.input}
                disableUnderline
            />
            <IconButton size="small" color="primary" onClick={() => onChange(value)}>
                <Search style={{fontSize: 22}}/>
            </IconButton>
        </Paper>
    );
};

SearchInput.propTypes = {
    className: PropTypes.string,
    onChange: PropTypes.func,
    style: PropTypes.object,
    initValue: PropTypes.string,
};

export default SearchInput;
