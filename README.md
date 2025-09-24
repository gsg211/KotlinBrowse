## Description

KotlinBrowse is a containerised minimal CLI browser made for as a homework exercise. It can browse the web and print out the content to the output. Additionally, it can block out websites written in banList.txt

## How to build and run

Download the source code then in the source directory run these commands.

If banList.txt is edited the image must be built again in order to update it.

##### Building

```
 sudo docker build --tag 'browser' .
```

#### Running
```
docker run -it browser
```

## How to use

Start the application. You will see a prompt showing the current URL. Enter commands to interact with the browser.

Available commands:

help
Displays a list of all available commands.

details
Shows detailed information about the project and each command.

search
Prompts you to enter a search term. Performs a DuckDuckGo search and displays a list of links.

link
Displays the list of links on the current page and lets you select one by index to navigate to.

links
Shows all links currently found on the active page.

content
Displays the main textual content of the current page.

html
Shows the raw HTML of the current page.

home
Navigates to the default homepage: https://html.duckduckgo.com/html/