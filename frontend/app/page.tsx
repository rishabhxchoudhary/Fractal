export default function HomePage() {
  // We read the environment variable to build the full backend URL.
  const backendUrl = `${process.env.NEXT_PUBLIC_API_URL}/oauth2/authorization/google`;

  return (
    <div className="flex h-screen w-screen items-center justify-center">
      <div className="text-center">
        <h1 className="mb-4 text-4xl font-bold">Welcome to Fractal</h1>
        <p className="mb-6 text-lg text-gray-600">
          Your new feature-rich Todo List application.
        </p>
        <a
          href={backendUrl}
          className="inline-block rounded-md bg-blue-600 px-6 py-3 font-semibold text-white shadow-md transition-transform duration-200 hover:scale-105"
        >
          Sign in with Google
        </a>
      </div>
    </div>
  );
}