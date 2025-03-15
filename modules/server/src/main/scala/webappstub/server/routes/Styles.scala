package webappstub.server.routes

object Styles:
  val content =
    "container mx-auto px-2 overflow-y-auto scrollbar-main scrollbar-thin flex-grow"

  val boxMd =
    "md:border dark:border-stone-500 bg-white dark:bg-stone-800 md:shadow-md"

  val inputLabel = "text-sm font-semibold py-0.5"

  val inputIcon = "absolute left-3 top-2 w-10 text-gray-400 dark:text-stone-400"

  val formFocusRing =
    "focus:ring focus:ring-black focus:ring-opacity-50 focus:ring-offset-0 dark:focus:ring-stone-400"

  val textInput =
    "placeholder-gray-400 w-full dark:text-stone-200 dark:bg-stone-800 dark:border-stone-500 " +
      s"border-gray-400 rounded border $formFocusRing"

  val searchInput =
    "border rounded my-1 dark:border-slate-700 border-grey-400 dark:bg-slate-700 dark:text-slate-200  px-1"

  val primaryButtonMain =
    "my-auto whitespace-nowrap bg-blue-500 border border-blue-500 dark:border-orange-800 dark:bg-orange-800 text-white text-center px-4 py-2 shadow-md focus:outline-none focus:ring focus:ring-opacity-75"

  val primaryButtonHover =
    "hover:bg-blue-600 dark:hover:bg-orange-700"

  val primaryButtonPlain =
    s"$primaryButtonMain $primaryButtonHover"

  val primaryButton =
    s"rounded $primaryButtonPlain"

  val errorMessage =
    " border border-red-600 bg-red-50 text-red-600 dark:border-orange-800 dark:bg-orange-300 dark:text-orange-800 px-2 py-2 rounded "

  val successMessage =
    " border border-green-600 bg-green-50 text-green-600 dark:border-lime-800 dark:bg-lime-300 dark:text-lime-800 px-4 py-2 rounded "

  val focusRingBtn =
    "focus:ring focus:ring-black focus:ring-opacity-50 focus:ring-offset-0 dark:focus:ring-stone-400"

  val borderBtn = "border border-gray-300 dark:border-stone-600"

  val basicBtnBg = "dark:bg-stone-800 bg-blue-50"

  val basicBtnHover =
    "hover:border-gray-300 dark:hover:border-stone-600 hover:bg-blue-100  dark:hover:text-white dark:hover:bg-stone-600 dark:hover:text-stone-100 cursor-pointer"

  val link = "text-blue-500 hover:text-blue-600 cursor-pointer"

  val btn = primaryButtonPlain

  val basicBtn = s"$basicBtnBg $basicBtnHover"
