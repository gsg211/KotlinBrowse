

import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.net.URLDecoder
import java.util.*
import kotlin.system.exitProcess


/**
 * Prototype design pattern
 * Used as the base for the requests
 */
class GenericRequest(  val url:String,   val param: MutableMap<String,String>): Cloneable
{
    public override fun clone(): Any {
        // Deep copy of params (to avoid shared mutable state)
        val clonedParams = param.toMutableMap()
        return GenericRequest(url, clonedParams)
    }
}

interface HttpGet
{
    fun getResponse():String
}

/**
 * Get request
 */
class GetRequest(  val url:String, public val param: MutableMap<String,String>,private val timeout:Int): HttpGet
{
    private val generic=GenericRequest(url,param)
    override fun getResponse():String
    {
        val clonedReq=generic.clone() as GenericRequest
        val con=Jsoup.connect(clonedReq.url)
            .method(Connection.Method.GET)
            .data(clonedReq.param)
            .userAgent("Mozilla/5.0")
            .timeout(timeout)
        con.execute()
        return con.response().body()
    }
}
/**
 * Get request with parental controls -> PROXY
 */
class ChildGetRequest( private val getRequest: GetRequest): HttpGet
{
    private var banList= mutableListOf<String>()

    override fun getResponse():String
    {
        getBanList()
        val response=getRequest.getResponse()

        for(item in banList)
        {
            val reg=Regex(item)
            if(reg.find(response)!=null)
            {
                println("PROBLEMA")
                return "ERROR"
            }
        }
        return response
    }

    fun getBanList()
    {
        val banWords = File("banList.txt").readLines()
        for(item in banWords)
        {
            banList.add(item)
        }
    }
    
}



/**
 * Post request
 */
class PostRequest( url:String, param: MutableMap<String,String>)
{
    private val generic=GenericRequest(url,param)
    fun getResponse():String
    {
        val clonedReq=generic.clone() as GenericRequest
        val con=Jsoup.connect(clonedReq.url)
            .method(Connection.Method.POST)
            .data(clonedReq.param)
            .userAgent("Mozilla/5.0")
            .post()

        return con.html()
    }
}
/**
    BROWSER FACADE
 */
class Browser(private val childProtection: Boolean)
{
    private var currentUrl="https://html.duckduckgo.com/html/"
    var timeout=10000;
    private var htmlContent=""

    fun getRequest(param1:String, param2:String)
    {
        val params= mutableMapOf<String,String>()
        params[param1]=param2
        val request=GetRequest(currentUrl,params,timeout)
        if(!childProtection)
        {
            htmlContent=request.getResponse()
            return
        }
        val childRequest=ChildGetRequest(request)
        htmlContent=childRequest.getResponse()
    }

    fun postRequest(param1:String, param2:String)
    {
        val params= mutableMapOf<String,String>()
        params[param1]=param2
        val request=PostRequest(currentUrl,params)

        htmlContent=request.getResponse()
    }

    fun search(search: String)
    {
        getRequest("q",search)
    }

    fun getCurrentUrl(): String
    {
        return this.currentUrl
    }

    fun getPlainText(): String
    {
        return this.htmlContent
    }

    fun getAllLinks(): MutableList<String>
    {
        val doc=Jsoup.parse(htmlContent)
        val links=doc.select("[href]")

        val wwwLinks = mutableListOf<String>()

        for (a in doc.select("a[href]")) {
            var href = a.attr("href")

            /*
                Decodes duck duck go redirects
             */

            if ("uddg=" in href) {
                val index = href.indexOf("uddg=")
                href = URLDecoder.decode(href.substring(index + 5), "UTF-8")
            }

            //val match = regex.find(href)
            if (href != null) {
                val cleanedUrl = href.split("&rut")[0]
                wwwLinks.add(cleanedUrl)
            }

        }
        //wwwLinks.forEach { println(it) }
        return wwwLinks
    }

    fun getContent(): MutableList<String>
    {
        val doc=Jsoup.parse(htmlContent)
        val content=doc.select("p")
        val paragraphs = mutableListOf<String>()
        for(paragraph in content)
        {
            paragraphs.add(paragraph.text())
        }
        println(paragraphs)
        return paragraphs
    }

    fun changeLink(newLink:String )
    {
        currentUrl=newLink
        getRequest("none","none")
    }
}

/**
 * FACADE WITH COMMAND LINE INTERFACE
 */
class cliBrowser(childProtection: Boolean) {
    private val browser = Browser(childProtection)

    fun start() {
        do {
            println("\nCurrent url: " + browser.getCurrentUrl())
            val cmd = readCommand()
            runCommand(cmd)
        } while (true)
    }

    private fun runCommand(command: String) {
        when (command) {
            "exit" -> Exit()
            "q" -> Exit()
            "search" -> search()
            "links" -> printAllLinks()
            "link" -> changeLink()
            "content" -> getContent()
            "html" -> getRawHtml()
            "home" -> home()
            "help" -> help()
            "get" -> customGetRequest()
            "post" -> customPostRequest()
            "details" -> detailedHelp()
            "goto" -> goToLink()  // New command to go directly to a link
            else -> println("Unknown command: '$command'. Type 'help' to see available commands.")
        }
    }

    private fun readCommand(): String {
        val scanner = Scanner(System.`in`)
        print("\nChoose a command\n>> ")
        return scanner.nextLine()
    }

    private fun Exit() {
        exitProcess(0)
    }

    fun help() {
        println(
            """
        === CLI Browser - Available Commands ===
        
        help       -> Displays this list of commands.
        details    -> Detailed information about the project and commands.
        search     -> Search for a term on DuckDuckGo and show the results.
        link       -> Select a link from the displayed list.
        links      -> Show all links found on the current page.
        content    -> Display the main textual content of the page.
        html       -> Show the raw HTML of the current page.
        home       -> Navigate to the homepage (DuckDuckGo).
        get        -> Send a custom GET request.
        post       -> Send a custom POST request.
        goto       -> Go directly to a specified URL.
        exit / q   -> Exit the application.
        
        Type 'details' for more in-depth help.
        """.trimIndent()
        )
    }

    fun detailedHelp() {
        println(
            """
        === CLI Browser - Project Details ===
        
        This project is a command-line interface browser built with a facade design pattern
        that wraps around a more complex 'Browser' class. It provides basic browsing features
        in a simplified and controlled environment. The browser also supports child protection
        filtering, making it safe for restricted use.
        
        === Command Descriptions ===
        
        help:
            Shows a short list of all available commands.
        
        details:
            Displays detailed information about the project and how each command works.
        
        search:
            Prompts the user for a search term, performs a DuckDuckGo search,
            and displays a list of resulting links.
        
        link:
            Lets the user select one of the displayed links (by index) to navigate to.
        
        links:
            Displays all links currently found on the active page.
        
        content:
            Prints only the main text content from the page (filtered).
        
        html:
            Prints the raw HTML source code of the current page.
        
        home:
            Navigates to the default homepage: https://html.duckduckgo.com/html/
        
        get / post:
            Sends a custom HTTP GET or POST request using two input parameters.
        
        goto:
            Lets you enter a direct URL to navigate to.
        
        exit / q:
            Closes the application safely.
        
        === Additional Info ===
        
        - Designed for educational or testing use cases in CLI environments.
        - Easy command-based navigation without a graphical interface.
        - The childProtection mode filters out unsafe or inappropriate content.
        
        """.trimIndent())
    }

    private fun printAllLinks() {
        var i = 0
        for (link in browser.getAllLinks()) {
            println("[$i] $link")
            i += 1
        }
    }

    private fun changeLink() {
        printAllLinks()
        val linkList = browser.getAllLinks()
        if (linkList.isEmpty()) {
            return
        }
        val scanner = Scanner(System.`in`)
        print("\nWhat link do you want to select? \n>> ")
        val link = scanner.nextInt()
        if (link >= linkList.size || link < 0) {
            return
        }
        browser.changeLink(linkList[link])
    }

    private fun search() {
        val scanner = Scanner(System.`in`)
        print("\nWhat do you want to search for ? \n>> ")
        val searchTerm = scanner.nextLine()
        browser.search(searchTerm)
        printAllLinks()
    }

    fun getRawHtml() {
        print(browser.getPlainText())
    }

    private fun getContent() {
        browser.getContent().forEach { println(it) }
    }

    private fun home() {
        browser.changeLink("https://html.duckduckgo.com/html/")
    }

    private fun customGetRequest() {
        val scanner = Scanner(System.`in`)
        print("\nCustom Get Request: \nParam 1 >> ")

        val parameter1 = scanner.nextLine()
        print("Parameter 2 >> ")
        val parameter2 = scanner.nextLine()

        browser.getRequest(parameter1, parameter2)
    }

    private fun customPostRequest() {
        val scanner = Scanner(System.`in`)
        print("\nCustom Post Request: \nParam 1 >> ")

        val parameter1 = scanner.nextLine()
        print("Parameter 2 >> ")
        val parameter2 = scanner.nextLine()

        browser.postRequest(parameter1, parameter2)
    }

    // New function to directly navigate to a URL
    private fun goToLink() {
        val scanner = Scanner(System.`in`)
        print("\nEnter the URL you want to visit: \n>> ")
        val url = scanner.nextLine()
        if (url.isNotEmpty()) {
            browser.changeLink(url)
        } else {
            println("Invalid URL. Please try again.")
        }
    }
}


fun main(args: Array<String>) {
    val browser=cliBrowser(false)
    browser.start()
}