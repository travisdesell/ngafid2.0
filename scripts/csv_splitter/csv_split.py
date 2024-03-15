#!/usr/bin/env python

import sys, getopt, re

header = ''
def get_date(datestr):
    return datestr.replace('/','_')

def get_time(timestr):
    l = timestr.split(':')
    return (timestr.replace(':','_'), l[0] + l[1])

def get_header(lines):
    l = 0
    global header
    for line in lines:
        if (is_header(line)):
            l = l + 1
            header = header + line
    return l
        
def is_header(line):
    #rexp to match header lines
    return re.match(r'[a-zA-Z#]{3}(.*?)', line)

def main(argv):
    global header
    if (len(argv) != 3):
        print("Usage: csv_split [csv_file_input] [min_time (minutes)]")
        print("Output files are generated automatically with the naming scheme:")
        print("flight_mm_dd_yy_hh_mm.csv")

    inputFile = ''

    try:
        inputFile = open(argv[1], 'r')
    except FileNotFoundError:
        print("file not found!", file = sys.stderr)
        sys.exit(1)

    print("Using file: " + inputFile.name)

    min_time = int(argv[2])
    lines = inputFile.readlines()
    outfile = ''
    ltime = sys.maxsize

    for i in range(get_header(lines),len(lines)):
        line = lines[i].replace('\n','')
        tokens = line.split(',')
        date = get_date(tokens[0])
        time = get_time(tokens[1]) 
        if (outfile == ''):
            print('creating file:')
            outfile = 'flight_' + date + '_' + time[0] + '.csv'
            outfile = open(outfile, 'w')
            outfile.write(header + '\n')
            print(outfile.name)

        if (abs(int(time[1]) - ltime) < min_time):
            outfile.write(line + '\n')
        else:
            outfile.close()
            outfile = ''
            ltime = sys.maxsize
            
        ltime = int(time[1])

if __name__ == "__main__":
    main(sys.argv)
